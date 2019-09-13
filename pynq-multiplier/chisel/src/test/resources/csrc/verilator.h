#ifndef _VERILATOR_H
#define _VERILATOR_H

class VerilatedVcdFILE : public VerilatedVcdFile {
 public:
  VerilatedVcdFILE(FILE* file) : file(file) {}
  ~VerilatedVcdFILE() {}
  bool open(const std::string& name) override {
    // file should already be open
    return file != NULL;
  }
  void close() override {
    // file should be closed elsewhere
  }
  ssize_t write(const char* bufp, ssize_t len) override {
    return fwrite(bufp, 1, len, file);
  }
 private:
  FILE* file;
};

#endif
