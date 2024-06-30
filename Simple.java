public class Simple {
  int addTwo(int x, int y) {
    return x + y;
  }

  public static void main(String[] args) {
    Simple simple = new Simple();

    int sum = simple.addTwo(3, 5);

    System.out.println(sum);
  }
}
