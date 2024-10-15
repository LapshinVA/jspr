package ru.netology;

import java.io.BufferedReader;

public class Body {
    private BufferedReader in;

    public Body(BufferedReader in) {
        this.in = in;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }
}
