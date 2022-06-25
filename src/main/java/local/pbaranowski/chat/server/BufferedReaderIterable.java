package local.pbaranowski.chat.server;

import java.io.*;
import java.util.Iterator;

public class BufferedReaderIterable implements Iterable<String> {
    private Iterator<String> iterator;

    public BufferedReaderIterable(BufferedReader br) {
        iterator = new BufferedReaderIterator(br);
    }

    public Iterator iterator() {
        return iterator;
    }

    private class BufferedReaderIterator implements Iterator<String> {
        private BufferedReader br;
        private java.lang.String line;

        public BufferedReaderIterator(BufferedReader aBR) {
            (br = aBR).getClass();
            advance();
        }

        public boolean hasNext() {
            return line != null;
        }

        public String next() {
            String retval = line;
            advance();
            return retval;
        }

        public void remove() {
            throw new UnsupportedOperationException("Unsupportet method.");
        }

        private void advance() {
            try {
                line = br.readLine();
            } catch (IOException e) { /* TODO */}
        }
    }
}
