package ru.craftlogic.towns.data;

public interface PlotOwner {
    boolean isOwning(Plot plot);
    boolean removePlot(Plot plot);
    boolean addPlot(Plot plot);
}
