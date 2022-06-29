package local.pbaranowski.chat.server;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Iterator;

@Slf4j
class BufferedReaderIterable implements Iterable<String> {
    private final Iterator<String> iterator;

    BufferedReaderIterable(BufferedReader bufferedReader) {
        iterator = new BufferedReaderIterator(bufferedReader);
    }

    @Override
    public Iterator<String> iterator() {
        return iterator;
    }

    private static class BufferedReaderIterator implements Iterator<String> {
        private final BufferedReader bufferedReader;
        private java.lang.String line;

        public BufferedReaderIterator(BufferedReader bufferedReader) {
            this.bufferedReader = bufferedReader;   //.getClass() ??
            advance();
        }

        @Override
        public boolean hasNext() {
            return line != null;
        }

        @Override
        public String next() {
            String retval = line;
            advance();
            return retval;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Unsupportet method.");
        }

        private void advance() {
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
