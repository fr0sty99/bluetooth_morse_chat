package ch.joris.morseapp;

import java.util.ArrayList;

public class MorsePattern {
    private ArrayList<Long> pattern;
    private int id;

    public MorsePattern(ArrayList<Long> pattern) {
        this.pattern = pattern;
    }

    public MorsePattern(int id, Long... times) {
        pattern = new ArrayList<Long>();
        for(Long time : times) {
            pattern.add(time);
        }
        this.id = id;
    }

    public MorsePattern(int id, ArrayList<Long> pattern) {
        this.pattern = pattern;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<Long> getPattern() {
        return pattern;
    }

    public int getPatternSize() {
        return pattern.size();
    }

    public ArrayList<Long> getMorsePattern() {
        return pattern;
    }

    public void setmorsePattern(ArrayList<Long> morsePattern) {
        this.pattern = morsePattern;
    }
}
