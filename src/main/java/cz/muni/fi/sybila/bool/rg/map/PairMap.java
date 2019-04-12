package cz.muni.fi.sybila.bool.rg.map;

public class PairMap {

    public static int tableResize = 0;
    public static int newBlock = 0;
    public static int resizeBlock = 0;

    private static final int initialSize = 4096;
    private static final int initialBlockSize = 18;
    private static final float loadFactor = 0.5f;

    private int size = 0;
    private int capacity = initialSize;
    private int[][] table = new int[initialSize][];

    public void put(int a, int b, int value) {
        put(table, a, b, value);
        if (size > capacity * loadFactor) resize();
    }

    public int get(int a, int b) {
        int[] block = table[hash(a, b) & (capacity - 1)];
        if (block == null) return -1;
        for (int i=0; i < block[0]; i++) {
            int position = 3*i + 1;
            if (block[position] == a && block[position+1] == b) {
                return block[position+2];
            }
        }
        return -1;
    }

    public void clear() {
        /*for (int i=0; i < table.length; i++) {
            int[] block = table[i];
            if (block != null) {
                table[i][0] = 0;
            }
        }
        size = 0;*/
        table = new int[capacity][];
        size = 0;
    }

    private void resize() {
        tableResize += 1;
        int[][] newTable = new int[2 * capacity][];
        for (int[] block : table) {
            if (block == null) continue;
            for (int j = 0; j < block[0]; j++) {
                int position = 3 * j + 1;
                put(newTable, block[position], block[position + 1], block[position + 2]);
            }
        }
        capacity = newTable.length;
        table = newTable;
    }

    private void put(int[][] table, int a, int b, int value) {
        int index = hash(a, b) & (capacity - 1);
        int[] block = table[index];
        if (block == null) {
            newBlock += 1;
            block = new int[initialBlockSize + 1];
            table[index] = block;
        }
        for (int i=0; i < block[0]; i++) {
            int position = 3*i + 1;
            if (block[position] == a && block[position+1] == b) {
                block[position+2] = value;
            }
        }
        int insertTo = 3*block[0] + 1;
        if (insertTo >= block.length) {
            resizeBlock += 1;
            int[] newBlock = new int[(block.length - 1) * 2 + 1];
            System.arraycopy(block, 0, newBlock, 0, block.length);
            table[index] = newBlock;
            block = newBlock;
        }
        block[insertTo] = a;
        block[insertTo+1] = b;
        block[insertTo+2] = value;
        block[0] += 1;
        size += 1;
    }

    private int hash(int i, int j) {
        return (((i+j) * (i + j + 1)) >>> 1) + 1;
    }

}
