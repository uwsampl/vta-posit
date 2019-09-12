// ins_count --> 16'h10;
// ins       --> 16'h14;
// uop       --> 16'h18;
// inp       --> 16'h1c;
// wgt       --> 16'h20;
// acc       --> 16'h24;
// out       --> 16'h28;

module tb;
  localparam HOST_ADDR_WIDTH = 32;
  localparam HOST_DATA_WIDTH = 32;
  localparam HOST_RESP_SIZE = 2;
  localparam HOST_A_ADDR = 32'hFFFC_0000;
  localparam HOST_B_ADDR = 32'hFFFD_0000;
  localparam HOST_C_ADDR = 32'hFFFF_0000;

  localparam VTA_START = 1;
  localparam VTA_DONE = 2;

  localparam VTA_DATA_LEN = 5;
  localparam VTA_DATA_BYTES = VTA_DATA_LEN * 8; // uint64_t = 8 bytes
  localparam VTA_BASE_ADDR = 32'h43C0_0000;

  wire clk;
  wire rst;

  reg ps_clk_r = 1'b0;
  reg ps_rstn_r;

  wire ps_clk;
  wire ps_rstn;

  always #10 ps_clk_r = ~ps_clk_r;

  assign ps_clk = ps_clk_r;
  assign ps_rstn = ps_rstn_r;

  task automatic vta_reset;
    input integer cycles;
    begin
      $display("[VTA] vta_reset start");
      ps_rstn_r = 0;
      tb.vta_sys.vta_i.processing_system7_1.inst.fpga_soft_reset(32'h1);
      repeat(cycles)@(negedge clk);
      ps_rstn_r = 1;
      tb.vta_sys.vta_i.processing_system7_1.inst.fpga_soft_reset(32'h0);
      wait(rst == 1'b0);
      $display("[VTA] vta_reset done");
    end
  endtask

  task automatic vta_read_reg;
    input [HOST_ADDR_WIDTH-1:0] addr;
    input              [16-1:0] offset;
    reg [HOST_ADDR_WIDTH-1:0] addr_;
    reg [HOST_DATA_WIDTH-1:0] data_;
    reg [HOST_RESP_SIZE-1:0] resp_;
    begin
      repeat(1)@(negedge clk);
      addr_ = addr + offset;
      data_ = 'hdeadbeef;
      tb.vta_sys.vta_i.processing_system7_1.inst.read_data(addr_, 8'h4, data_, resp_);
      $display("[VTA] vta_read_reg addr:%x data:%x resp:%x", addr_, data_, resp_);
    end
  endtask

  task automatic vta_write_reg;
    input [HOST_ADDR_WIDTH-1:0] addr;
    input              [16-1:0] offset;
    input [HOST_DATA_WIDTH-1:0] data;
    reg [HOST_ADDR_WIDTH-1:0] addr_;
    reg [HOST_RESP_SIZE-1:0] resp_;
    begin
      repeat(1)@(negedge clk);
      addr_ = addr + offset;
      tb.vta_sys.vta_i.processing_system7_1.inst.write_data(addr_, 8'h4, data, resp_);
      $display("[VTA] vta_write_reg addr:%x data:%x resp:%x", addr_, data, resp_);
    end
  endtask

  task automatic vta_load_data;
    begin
      tb.vta_sys.vta_i.processing_system7_1.inst.pre_load_mem_from_file("a.mem", HOST_A_ADDR, VTA_DATA_BYTES);
      tb.vta_sys.vta_i.processing_system7_1.inst.pre_load_mem_from_file("b.mem", HOST_B_ADDR, VTA_DATA_BYTES);
    end
  endtask

  task automatic vta_wait_until_completion;
    input integer cycles;
    input integer times;
    reg [HOST_DATA_WIDTH-1:0] data_;
    reg [HOST_RESP_SIZE-1:0] resp_;
    integer i;
    begin
      for (i = 0; i < times; i = i + 1) begin
        repeat(cycles)@(negedge clk);
        data_ = 0;
        tb.vta_sys.vta_i.processing_system7_1.inst.read_data(VTA_BASE_ADDR, 8'h4, data_, resp_);
        if (data_ & 'h2) begin
          tb.vta_sys.vta_i.processing_system7_1.inst.peek_mem_to_file("c.mem", HOST_C_ADDR, VTA_DATA_BYTES);
          $display("\n[VTA] call finish\n");
          $finish;
        end
      end
    end
  endtask

  assign clk = tb.vta_sys.vta_i.clock_net;
  assign rst = ~tb.vta_sys.vta_i.reset_net;

  initial begin
    $display("[VTA] Simulation start");

    vta_load_data();
    vta_reset(100);
    vta_write_reg(VTA_BASE_ADDR, 'h04, 'h0);
    vta_write_reg(VTA_BASE_ADDR, 'h08, VTA_DATA_LEN);
    vta_write_reg(VTA_BASE_ADDR, 'h0c, HOST_A_ADDR);
    vta_write_reg(VTA_BASE_ADDR, 'h10, HOST_B_ADDR);
    vta_write_reg(VTA_BASE_ADDR, 'h14, HOST_C_ADDR);
    vta_write_reg(VTA_BASE_ADDR, 'h00, VTA_START);
    vta_wait_until_completion(1000, 200);

    $display("[VTA] Simulation done");
    $finish;
  end

  vta_wrapper vta_sys
  (
    .DDR_addr(),
    .DDR_ba(),
    .DDR_cas_n(),
    .DDR_ck_n(),
    .DDR_ck_p(),
    .DDR_cke(),
    .DDR_cs_n(),
    .DDR_dm(),
    .DDR_dq(),
    .DDR_dqs_n(),
    .DDR_dqs_p(),
    .DDR_odt(),
    .DDR_ras_n(),
    .DDR_reset_n(),
    .DDR_we_n(),
    .FIXED_IO_ddr_vrn(),
    .FIXED_IO_ddr_vrp(),
    .FIXED_IO_mio(),
    .FIXED_IO_ps_clk(ps_clk),
    .FIXED_IO_ps_porb(ps_rstn),
    .FIXED_IO_ps_srstb(ps_rstn)
  );

endmodule
