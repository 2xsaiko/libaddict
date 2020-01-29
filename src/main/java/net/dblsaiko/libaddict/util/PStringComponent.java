package net.dblsaiko.libaddict.util;

public interface PStringComponent {

    void fmt(StringBuilder sb, Object[] params);

    String asString();

    static Verbatim verbatim(String value) {
        return new Verbatim(value);
    }

    static Variable variable(int idx) {
        return new Variable(idx);
    }

    class Verbatim implements PStringComponent {

        public final String value;

        Verbatim(String value) {
            this.value = value;
        }

        @Override
        public void fmt(StringBuilder sb, Object[] params) {
            sb.append(value);
        }

        @Override
        public String asString() {
            return value;
        }

    }

    class Variable implements PStringComponent {

        public final int idx;

        Variable(int idx) {
            this.idx = idx;
        }

        @Override
        public void fmt(StringBuilder sb, Object[] params) {
            sb.append(params[idx].toString());
        }

        @Override
        public String asString() {
            return String.format("{%d}", idx);
        }

    }

}
