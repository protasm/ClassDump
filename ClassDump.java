import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ClassDump {
  private Map<Integer, String> constantPoolValues;

  ClassDump() {
    this.constantPoolValues = new HashMap<Integer, String>();
  }

  private void dumpClassFile(InputStream input) throws IOException {
    byte[] magicWordBytes = readBytes(input, 4);

    if (!Arrays.equals(
      magicWordBytes,
      new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE }
    )) {
      System.out.println("Not a Java class file.");

      return;
    }

    printBytesWithComment(magicWordBytes, bytesToHexString(magicWordBytes), "Magic number: ", -1);
    System.out.println();

    byte[] minorVersionBytes = readBytes(input, 2);
    int minorVersion = twoBytesToUnsignedInt(minorVersionBytes);
    printBytesWithComment(minorVersionBytes, String.format("%d", minorVersion), "Minor version: ", -1);
    System.out.println();

    byte[] majorVersionBytes = readBytes(input, 2);
    int majorVersion = twoBytesToUnsignedInt(majorVersionBytes);
    printBytesWithComment(majorVersionBytes, String.format("%d", majorVersion), "Major version: ", -1);
    System.out.println();

    byte[] constantPoolCountBytes = readBytes(input, 2);
    int constantPoolCount = twoBytesToUnsignedInt(constantPoolCountBytes);
    printBytesWithComment(constantPoolCountBytes, String.format("%d", constantPoolCount), "Constant pool count + 1: ", -1);
    System.out.println();

    System.out.println("==CONSTANT POOL==");

    for (int i = 1; i < constantPoolCount;) {
      i = i + printConstantPoolEntry(i, input);

      System.out.println();
    }

    System.out.println("==END CONSTANT POOL==");
    System.out.println();

    byte[] accessFlagsBytes = readBytes(input, 2);
    int accessFlags = twoBytesToUnsignedInt(accessFlagsBytes);
    printBytesWithComment(accessFlagsBytes, String.format("0x%04x", accessFlags), "Access Flags: ", -1);
    System.out.println();

    byte[] thisClassIndexBytes = readBytes(input, 2);
    int thisClassIndex = twoBytesToUnsignedInt(thisClassIndexBytes);
    printBytesWithComment(thisClassIndexBytes, String.format("%d", thisClassIndex), "This Class @ #", -1);
    System.out.println();

    byte[] superClassIndexBytes = readBytes(input, 2);
    int superClassIndex = twoBytesToUnsignedInt(superClassIndexBytes);
    printBytesWithComment(superClassIndexBytes, String.format("%d", superClassIndex), "Super Class @ #", -1);
    System.out.println();

    System.out.println("==INTERFACES==");

    byte[] interfacesCountBytes = readBytes(input, 2);
    int interfacesCount = twoBytesToUnsignedInt(interfacesCountBytes);
    printBytesWithComment(interfacesCountBytes, String.format("%d", interfacesCount), "Interfaces count: ", -1);
    System.out.println();

    if (interfacesCount > 0) {
      for (int i = 0; i < interfacesCount; i++) {
        byte[] interfaceIndexBytes = readBytes(input, 2);
        int interfaceIndex = twoBytesToUnsignedInt(interfaceIndexBytes);
        printBytesWithComment(interfaceIndexBytes, String.format("%d", interfaceIndex), "Interface @ #", interfaceIndex);

        System.out.println();
      }
    }

    System.out.println("==END INTERFACES==");
    System.out.println();

    System.out.println("==FIELDS==");

    byte[] fieldsCountBytes = readBytes(input, 2);
    int fieldsCount = twoBytesToUnsignedInt(fieldsCountBytes);
    printBytesWithComment(fieldsCountBytes, String.format("%d", fieldsCount), "Fields count: ", -1);
    System.out.println();

    if (fieldsCount > 0) {
      for (int i = 0; i < fieldsCount; i++) {
        printFieldOrMethod(input, "Field");
      }

      System.out.println();
    }

    System.out.println("==END FIELDS==");
    System.out.println();

    System.out.println("==METHODS==");

    byte[] methodsCountBytes = readBytes(input, 2);
    int methodsCount = twoBytesToUnsignedInt(methodsCountBytes);

    printBytesWithComment(methodsCountBytes, String.format("%d", methodsCount), "Methods count: ", -1);
    System.out.println();

    if (methodsCount > 0) {
      for (int i = 0; i < methodsCount; i++) {
        printFieldOrMethod(input, "Method");
      }
    
      System.out.println();
    }

    System.out.println("==END METHODS==");
    System.out.println();

    System.out.println("==ATTRIBUTES==");

    byte[] attributesCountBytes = readBytes(input, 2);
    int attributesCount = twoBytesToUnsignedInt(attributesCountBytes);
    printBytesWithComment(attributesCountBytes, String.format("%d", attributesCount), "Attributes count: ", -1);
    System.out.println();

    System.out.println("Attributes:");
    for (int i = 0; i < attributesCount; i++) {
      printAttribute(input);
    }

    System.out.println("==END ATTRIBUTES==");
    System.out.println();
  }

  private void printFieldOrMethod(InputStream input, String type) throws IOException {
    byte[] accessFlagsBytes = readBytes(input, 2);
    int accessFlags = twoBytesToUnsignedInt(accessFlagsBytes);
    printBytesWithComment(accessFlagsBytes, String.format("0x%04x", accessFlags), "Access flags: ", -1);

    byte[] nameIndexBytes = readBytes(input, 2);
    int nameIndex = twoBytesToUnsignedInt(nameIndexBytes);
    printBytesWithComment(nameIndexBytes, String.format("%d", nameIndex), "Name @ #", nameIndex);

    byte[] descriptorIndexBytes = readBytes(input, 2);
    int descriptorIndex = twoBytesToUnsignedInt(descriptorIndexBytes);
    printBytesWithComment(descriptorIndexBytes, String.format("%d", descriptorIndex), "Descriptor @ #", descriptorIndex);

    byte[] attributesCountBytes = readBytes(input, 2);
    int attributesCount = twoBytesToUnsignedInt(attributesCountBytes);
    printBytesWithComment(attributesCountBytes, String.format("%d", attributesCount), "Attributes count: ", -1);

    for (int j = 0; j < attributesCount; j++) {
      printAttribute(input);
    }

    System.out.println();
  }

  private void printAttribute(InputStream input) throws IOException {
    byte[] attributeNameIndexBytes = readBytes(input, 2);
    int attributeNameIndex = twoBytesToUnsignedInt(attributeNameIndexBytes);
    printBytesWithComment(attributeNameIndexBytes, String.format("%d", attributeNameIndex), "Attribute name @ #", attributeNameIndex);

    byte[] attributeLengthBytes = readBytes(input, 4);
    long attributeLength = fourBytesToUnsignedLong(attributeLengthBytes);
    printBytesWithComment(attributeLengthBytes, String.format("%d", attributeLength), "Attribute length: ", -1);

    String attributeName = this.constantPoolValues.get(attributeNameIndex);
    decodeAttributeInfo(input, attributeName, attributeLength);

    System.out.print("\n"); //end line
  }

  private void decodeAttributeInfo(InputStream input, String name, long length) throws IOException {

    if (!name.equals("Code")) {
      System.out.println("// Attribute info:");

      for (int i = 0; i < length; i++) {
        System.out.printf("%02x ", input.read());
  
        if ((i + 1) % 32 == 0) {
          System.out.print("\n"); //end line
        }
      }

      return;
    }

    //Decode Code attribute
    System.out.println("==CODE==");

    byte[] maxStackBytes = readBytes(input, 2);
    int maxStack = twoBytesToUnsignedInt(maxStackBytes);
    printBytesWithComment(maxStackBytes, String.format("%d", maxStack), "Max stack: ", -1);

    byte[] maxLocalsBytes = readBytes(input, 2);
    int maxLocals = twoBytesToUnsignedInt(maxLocalsBytes);
    printBytesWithComment(maxLocalsBytes, String.format("%d", maxLocals), "Max locals: ", -1);

    byte[] codeLengthBytes = readBytes(input, 4);
    long codeLength = fourBytesToUnsignedLong(codeLengthBytes);
    printBytesWithComment(codeLengthBytes, String.format("%d", codeLength), "Code length: ", -1);

    System.out.println("// Code bytes:");
    for (int i = 0; i < codeLength; i++) {
      if (i != 0 && i % 32 == 0) { System.out.print("\n"); }

      System.out.printf("%02x ", input.read());
    }
    System.out.println();

    byte[] exceptionTableLengthBytes = readBytes(input, 2);
    int exceptionTableLength = twoBytesToUnsignedInt(exceptionTableLengthBytes);
    printBytesWithComment(exceptionTableLengthBytes, String.format("%d", exceptionTableLength), "Exception table length: ", -1);

    //need code here to handle exceptionTableLength > 0

    byte[] attributesCountBytes = readBytes(input, 2);
    int attributesCount = twoBytesToUnsignedInt(attributesCountBytes);
    printBytesWithComment(attributesCountBytes, String.format("%d", attributesCount), "Attributes count: ", -1);

    System.out.println("// Remaining method bytes:");
    for (long i = 0; i < length - codeLength - 4 - 2 - 2 - 2 - 2; i++) {
      if (i != 0 && i % 32 == 0) { System.out.print("\n"); }

      System.out.printf("%02x ", input.read());
    }
  }

  public String intToHexString(int i) {
    return String.format("%02x", i);
  }

  public String byteToHexString(byte b) {
    return String.format("%02x", b & 0xFF);
  }

  public String bytesToHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < bytes.length; i++) {
        sb.append(byteToHexString(bytes[i]));

        if (i < bytes.length - 1) {
            sb.append(" ");
        }
    }

    return sb.toString();
  }

  private void printBytesWithComment(byte[] bytes, String comment, String label, int index) throws IOException {
    if (index > 0) {
      String value = this.constantPoolValues.get(index);

      System.out.println("// " + label + comment + " [\"" + value + "\"]");
    } else {
      System.out.println("// " + label + comment);
    }

    System.out.println(bytesToHexString(bytes));
  }

  private int twoBytesToUnsignedInt(byte[] bytes) throws IOException {
    int value = 0;
    
    value |= (bytes[0] & 0xFF) << 8;
    value |= bytes[1] & 0xFF;
    
    return value;
  }

  private long fourBytesToUnsignedLong(byte[] bytes) throws IOException {
    return new BigInteger(bytes).longValue();
  }

  private int constantPoolEntrySize(int tag) {
    switch (tag) {
      case 1:  // Utf8
        return 2;  // Length of bytes followed by the bytes themselves
      case 3:  // Integer
      case 4:  // Float
        return 4;
      case 5:  // Long
      case 6:  // Double
        return 8;
      case 7:  // Class
      case 8:  // String
        return 2;
      case 9:  // Fieldref
      case 10: // Methodref
      case 11: // InterfaceMethodref
      case 12: // NameAndType
        return 4;
      case 15: // MethodHandle
        return 3;
      case 16: // MethodType
        return 2;
      case 18: // InvokeDynamic
        return 4;
      default:
        throw new IllegalArgumentException("Invalid constant pool tag: " + tag);
    }
  }

  private byte[] readBytes(InputStream input, int numBytes) throws IOException {
    byte[] buffer = new byte[numBytes];

    input.read(buffer);

    return buffer;
  }

  private int printConstantPoolEntry(int entryNum, InputStream input) throws IOException {
    int tag = input.read();

    switch (tag) {
      case 1: { //Utf8
        byte[] w = readBytes(input, 2);
        int length = twoBytesToUnsignedInt(w);
        byte[] bytes = new byte[length];
        int bytesRead = input.read(bytes);
        String value = new String(bytes);

        System.out.println("#" + entryNum + " //Utf8: \"" + value + "\" (" + bytesRead + " bytes)");
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(bytes));

        this.constantPoolValues.put(entryNum, value);

        return 1;
      }

      case 3: { //Integer
        byte[] dw = readBytes(input, 4);
        long value = fourBytesToUnsignedLong(dw);

        System.out.println("#" + entryNum + " //Integer: " + value);
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(dw));

        return 1;
      }

      case 4: { //Float
        byte[] dw = readBytes(input, 4);
        long value = fourBytesToUnsignedLong(dw);

        System.out.println("#" + entryNum + " //Float: " + Float.intBitsToFloat((int) value));
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(dw));

        return 1;
      }

      case 5: { //Long
        byte[] dw1 = readBytes(input, 4);
        byte[] dw2 = readBytes(input, 4);
        long value = (fourBytesToUnsignedLong(dw1) << 32) | (fourBytesToUnsignedLong(dw2) & 0xFFFFFFFFL);

        System.out.println("#" + entryNum + " //Long: " + value);
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(dw1) + ", " + bytesToHexString(dw2));

        return 2;
      }

      case 6: { //Double
        byte[] dw1 = readBytes(input, 4);
        byte[] dw2 = readBytes(input, 4);
        long value = (fourBytesToUnsignedLong(dw1) << 32) | (fourBytesToUnsignedLong(dw2) & 0xFFFFFFFFL);

        System.out.println("#" + entryNum + " //Double: " + Double.longBitsToDouble(value));
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(dw1) + ", " + bytesToHexString(dw2));

        return 2;
      }

      case 7: { //Class: Name (index)
        byte[] w = readBytes(input, 2);
        int index = twoBytesToUnsignedInt(w);

        System.out.println("#" + entryNum + " //Class: Name @ #" + index + " [Utf8]");
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w));

        return 1;
      }

      case 8: { //String (index)
        byte[] w = readBytes(input, 2);
        int index = twoBytesToUnsignedInt(w);

        System.out.println("#" + entryNum + " //String @ #" + index + " [Utf8]");
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w));

        return 1;
      }

      case 9: { //Fieldref: Class (index), NameAndType (index)
        byte[] w1 = readBytes(input, 2);
        byte[] w2 = readBytes(input, 2);
        int classIndex = twoBytesToUnsignedInt(w1);
        int nameAndTypeIndex = twoBytesToUnsignedInt(w2);

        System.out.println("#" + entryNum + " //Fieldref: Class @ #" + classIndex + ", NameAndType @ #" + nameAndTypeIndex);
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w1) + ", " + bytesToHexString(w2));

        return 1;
      }

      case 10: { //Methodref: Class (index), NameAndType (index)
        byte[] w1 = readBytes(input, 2);
        byte[] w2 = readBytes(input, 2);
        int classIndex = twoBytesToUnsignedInt(w1);
        int nameAndTypeIndex = twoBytesToUnsignedInt(w2);

        System.out.println("#" + entryNum + " //Methodref: Class @ #" + classIndex + ", NameAndType @ #" + nameAndTypeIndex);
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w1) + ", " + bytesToHexString(w2));

        return 1;
      }

      case 11: { //InterfaceMethodref: Class (index), NameAndType (index)
        byte[] w1 = readBytes(input, 2);
        byte[] w2 = readBytes(input, 2);
        int classIndex = twoBytesToUnsignedInt(w1);
        int nameAndTypeIndex = twoBytesToUnsignedInt(w2);

        System.out.println("#" + entryNum + " //InterfaceMethodref: Class @ #" + classIndex + ", NameAndType @ #" + nameAndTypeIndex);
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w1) + ", " + bytesToHexString(w2));

        return 1;
      }

      case 12: { //NameAndType: Name (index), Descriptor (index)
        byte[] w1 = readBytes(input, 2);
        byte[] w2 = readBytes(input, 2);
        int nameIndex = twoBytesToUnsignedInt(w1);
        int descriptorIndex = twoBytesToUnsignedInt(w2);

        System.out.println("#" + entryNum + " //NameAndType: Name @ #" + nameIndex + " [Utf8], Type Descriptor @ #" + descriptorIndex + " [Utf8]");
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w1) + ", " + bytesToHexString(w2));

        return 1;
      }

      case 15: { //MethodHandle: ReferenceKind, Reference (index)
        int i = input.read();
        byte[] w = readBytes(input, 2);
        int referenceIndex = twoBytesToUnsignedInt(w);

        System.out.println("#" + entryNum + " //MethodHandle: ReferenceKind " + i + ", Reference @ #" + referenceIndex);
        System.out.println(intToHexString(tag) + ": " + intToHexString(i) + ", " + bytesToHexString(w));

        return 1;
      }

      case 16: { //MethodType: Descriptor (index)
        byte[] w = readBytes(input, 2);
        int descriptorIndex = twoBytesToUnsignedInt(w);

        System.out.println("#" + entryNum + " //MethodType: Type Descriptor @ #" + descriptorIndex + " [Utf8]");
        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w));

        return 1;
      }

      case 18: { //InvokeDynamic: BootstrapMethodAttr (index), NameAndType (index)
        byte[] w1 = readBytes(input, 2);
        byte[] w2 = readBytes(input, 2);
        int bootstrapMethodAttrIndex = twoBytesToUnsignedInt(w1);
        int nameAndTypeIndex = twoBytesToUnsignedInt(w2);

        System.out.println(
          "#" + entryNum +
          " //InvokeDynamic: BootstrapMethodAttr @ #" + bootstrapMethodAttrIndex +
          ", NameAndType @ #" + nameAndTypeIndex
        );

        System.out.println(intToHexString(tag) + ": " + bytesToHexString(w1) + ", " + bytesToHexString(w2));

        return 1;
      }

      default:
        throw new IllegalArgumentException("Invalid constant pool tag: " + tag);
    }
  }

  private void dumpEntireHexContents(String classFileName, InputStream input) throws IOException {
    System.out.println("Hex Dump of " + classFileName + ":");

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];

    while ((nRead = input.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }

    buffer.flush();

    byte[] byteArray = buffer.toByteArray();

    for (int i = 0; i < byteArray.length; i++) {
      System.out.printf("%02x ", byteArray[i]);

      if ((i + 1) % 32 == 0) {
        System.out.print("\n"); //blank line
      }
    }

    System.out.print("\n"); //end line
    System.out.print("\n"); //blank line
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Usage: java ClassDump <class file>");

      return;
    }

    String filename = args[0];

    ClassDump cd = new ClassDump();

    try (InputStream input = new FileInputStream(filename)) {
      cd.dumpEntireHexContents(filename, input);
    }

    // Reopen the input stream to process it again
    try (InputStream input = new FileInputStream(filename)) {
      cd.dumpClassFile(input);
    }
  }
}

