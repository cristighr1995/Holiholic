package com.holiholic.planner.utils;

public class WeatherForecastInformation {
    public double temperature;
    public double rainProbability;
    public double snowProbability;

    public WeatherForecastInformation(double temperature, double rainProbability, double snowProbability) {
        this.temperature = temperature;
        this.rainProbability = rainProbability;
        this.snowProbability = snowProbability;
    }
}
