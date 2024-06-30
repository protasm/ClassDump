class HW {
  String bar;

  HW() {
    this.bar = "default: bar";
  }

  HW(String bar) {
    this.bar = bar;
  }

  public static void main(String[] args) {
    HW hw;

    if (args.length > 0) {
      hw = new HW(args[0]);
    } else {
      hw = new HW();
    }

    hw.foo(); 
  }

  private void foo() {
    System.out.println(this.bar);
  }
}
