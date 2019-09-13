#if VM_TRACE
#include <memory>
#include <verilated_vcd_c.h>
#include "verilator.h"
#endif

#include <iostream>
#include <getopt.h>

static uint64_t trace_count = 0;

static void usage(const char * program_name)
{
  printf("\nVTA simulator usage:\n");
  printf("-h, --help               Display this help and exit\n");
  printf("-m, --max-cycles=CYCLES  Finish simulation after CYCLES\n");
#if VM_TRACE
  printf("-v, --vcd=FILE,          Write vcd trace to FILE (or '-' for stdout)\n");
  printf("-x, --dump-start=CYCLE   Start VCD tracing at CYCLE\n");
#endif
}

int main(int argc, char **argv)
{

  uint64_t max_cycles = -1;

#if VM_TRACE
  FILE * vcdfile = NULL;
  uint64_t start = 0;
#endif

  static struct option long_options[] =
  {
    {"help",        no_argument,       0, 'h' },
    {"max-cycles",  required_argument, 0, 'm' },
#if VM_TRACE
    {"vcd",         required_argument, 0, 'v' },
    {"dump-start",  required_argument, 0, 'x' },
#endif
  };

  int option_index = 0;
  int c;

#if VM_TRACE
  while ((c = getopt_long(argc, argv, "-hm:v:x:", long_options, &option_index)) != -1)
#else
  while ((c = getopt_long(argc, argv, "-hm:", long_options, &option_index)) != -1)
#endif
  {
    switch (c)
    {
      case '?': usage(argv[0]);             return 1;
      case 'h': usage(argv[0]);             return 0;
      case 'm': max_cycles = atoll(optarg); break;
#if VM_TRACE
      case 'v':
      {
        vcdfile = strcmp(optarg, "-") == 0 ? stdout : fopen(optarg, "w");
        if (!vcdfile)
        {
          std::cerr << "Unable to open " << optarg << " for VCD write\n";
          return 1;
        }
        break;
      }
      case 'x': start = atoll(optarg);      break;
#endif
    }
  }

  Verilated::commandArgs(argc, argv);
  VNAME *top = new VNAME;

#if VM_TRACE
  Verilated::traceEverOn(true);
  std::unique_ptr<VerilatedVcdFILE> vcdfd(new VerilatedVcdFILE(vcdfile));
  std::unique_ptr<VerilatedVcdC> tfp(new VerilatedVcdC(vcdfd.get()));
  if (vcdfile) {
    top->trace(tfp.get(), 99);
    tfp->open("");
  }
#endif

  // reset
  for (int i = 0; i < 10; i++)
  {
    top->reset = 1;
    top->clock = 0;
    top->eval();
#if VM_TRACE
    if (trace_count >= start)
      tfp->dump(static_cast<vluint64_t>(trace_count * 2));
#endif
    top->clock = 1;
    top->eval();
#if VM_TRACE
    if (trace_count >= start)
      tfp->dump(static_cast<vluint64_t>(trace_count * 2 + 1));
#endif
    trace_count++;
  }
  top->reset = 0;

  // start simulation
  while (!Verilated::gotFinish() && trace_count < max_cycles)
  {
    top->clock = 0;
    top->eval();
#if VM_TRACE
    if (trace_count >= start)
      tfp->dump(static_cast<vluint64_t>(trace_count * 2));
#endif
    top->clock = 1;
    top->eval();
#if VM_TRACE
    if (trace_count >= start)
      tfp->dump(static_cast<vluint64_t>(trace_count * 2 + 1));
#endif
    trace_count++;
  }

#if VM_TRACE
  if (tfp)
    tfp->close();
  if (vcdfile)
    fclose(vcdfile);
#endif

  delete top;

  exit(0);
}
