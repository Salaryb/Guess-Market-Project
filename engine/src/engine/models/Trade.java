package engine.models;

import java.io.Serializable;

public record Trade(String optionName, int shares, double pricePaid) implements Serializable {}