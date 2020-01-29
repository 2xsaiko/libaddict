package net.dblsaiko.libaddict;

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;

public class ParameterizedString {

    private final List<PStringComponent> components;

    private ParameterizedString(List<PStringComponent> components) {
        this.components = components;
    }

    public String fmt(Object[] params) {
        if (components.size() == 0) return "";
        if (components.size() == 1 && components.get(0) instanceof PStringComponent.Verbatim) {
            return ((PStringComponent.Verbatim) components.get(0)).value;
        }

        StringBuilder sb = new StringBuilder();
        for (PStringComponent component : components) {
            component.fmt(sb, params);
        }
        return sb.toString();
    }

    public static ParameterizedString from(String s) {
        List<PStringComponent> components = new ArrayList<>();
        PrimitiveIterator.OfInt iter = s.chars().iterator();
        StringBuilder cur = new StringBuilder();
        int counter = 0;
        while (iter.hasNext()) {
            char c = (char) iter.nextInt();
            switch (c) {
                case '{':
                    StringBuilder b = new StringBuilder();
                    while (true) {
                        if (iter.hasNext()) {
                            char d = (char) iter.nextInt();
                            if (d == '}') break;
                            b.append(d);
                        } else {
                            throw new IllegalStateException("Unexpected EOF");
                        }
                    }
                    String bStr = b.toString();
                    int index;
                    if ("".equals(bStr)) {
                        index = counter;
                        counter += 1;
                    } else {
                        index = Integer.parseInt(bStr);
                    }
                    String curStr = cur.toString();
                    if (!curStr.isEmpty()) {
                        components.add(PStringComponent.verbatim(curStr));
                        cur = new StringBuilder();
                    }
                    components.add(PStringComponent.variable(index));
                    break;
                case '\\':
                    if (iter.hasNext()) {
                        cur.append((char) iter.nextInt());
                    } else {
                        throw new IllegalStateException("Unexpected EOF");
                    }
                    break;
                default:
                    cur.append(c);
            }
        }
        String curStr = cur.toString();
        if (!curStr.isEmpty()) {
            components.add(PStringComponent.verbatim(curStr));
        }
        return new ParameterizedString(components);
    }

}
