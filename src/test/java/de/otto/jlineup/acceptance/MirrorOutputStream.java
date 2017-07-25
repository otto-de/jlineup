package de.otto.jlineup.acceptance;

import java.io.IOException;
import java.io.OutputStream;

public final class MirrorOutputStream extends OutputStream {

    private final OutputStream one;
    private final OutputStream two;

    MirrorOutputStream(OutputStream out, OutputStream tee) {
        if (out == null || tee == null) {
            throw new NullPointerException();
        }
        this.one = out;
        this.two = tee;
    }

    @Override
    public void write(int b) throws IOException {
        one.write(b);
        two.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        one.write(b);
        two.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        one.write(b, off, len);
        two.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        one.flush();
        two.flush();
    }

    @Override
    public void close() throws IOException {
        Exception oneEx = null;
        Exception twoEx = null;
        try {
            one.close();
        } catch (IOException o) {
            oneEx = o;
        }
        try {
            two.close();
        } catch (IOException t) {
            twoEx = t;
        }
        if (oneEx != null) {
            try {
                throw oneEx;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (twoEx != null) {
            try {
                throw twoEx;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
