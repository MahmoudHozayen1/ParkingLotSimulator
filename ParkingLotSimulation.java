package com.example;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ParkingLotSimulation {

    public static void main(String[] args) {

        int totalParkingSpots = 4;
        Semaphore parkingSpots = new Semaphore(totalParkingSpots);
        AtomicInteger carsServed = new AtomicInteger(0);
        Map<String, AtomicInteger> gateCarCounts = new HashMap<>();
        Queue<String> logs = new ConcurrentLinkedQueue<>();

        List<String> carDetails = readInputFile("demo/input/cars.txt");
        if (carDetails == null) {
            System.out.println("Could not load the input file.");
            return;
        }

        gateCarCounts.put("Gate 1", new AtomicInteger(0));
        gateCarCounts.put("Gate 2", new AtomicInteger(0));
        gateCarCounts.put("Gate 3", new AtomicInteger(0));

        List<Thread> carThreads = new ArrayList<>();
        for (String car : carDetails) {
            Thread carThread = createCarThread(car, parkingSpots, carsServed, gateCarCounts, logs);
            if (carThread != null) {
                carThreads.add(carThread);
            }
        }

        for (Thread carThread : carThreads) {
            carThread.start();
        }

        for (Thread carThread : carThreads) {
            try {
                carThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Simulation completed. Total cars served: " + carsServed.get());
        for (Map.Entry<String, AtomicInteger> entry : gateCarCounts.entrySet()) {
            System.out.println(entry.getKey() + " served " + entry.getValue() + " cars.");
        }

        System.out.println("\nDetailed logs:");
        for (String log : logs) {
            System.out.println(log);
        }
    }

    private static List<String> readInputFile(String filePath) {
        List<String> carEntries = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                carEntries.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return carEntries;
    }

    private static Thread createCarThread(String data, Semaphore parkingSpots, AtomicInteger carsServed,
                                          Map<String, AtomicInteger> gateCarCounts, Queue<String> logs) {

        String[] details = data.split(",");
        if (details.length < 4) {
            System.out.println("Invalid data format: " + data);
            return null;
        }

        String gate = details[0].trim();
        String carIdStr = details[1].trim();
        if (!carIdStr.startsWith("Car ") || !isNumeric(carIdStr.substring(4))) {
            System.out.println("Invalid carId format: " + details[1]);
            return null;
        }
        int carId = Integer.parseInt(carIdStr.substring(4).trim());

        String arrivalTimeStr = details[2].trim();
        if (!arrivalTimeStr.startsWith("Arrive ") || !isNumeric(arrivalTimeStr.substring(7))) {
            System.out.println("Invalid arrivalTime format: " + details[2]);
            return null;
        }
        int arrivalTime = Integer.parseInt(arrivalTimeStr.substring(7).trim());

        String parkingDurationStr = details[3].trim();
        if (!parkingDurationStr.startsWith("Parks ") || !isNumeric(parkingDurationStr.substring(6))) {
            System.out.println("Invalid parkingDuration format: " + details[3]);
            return null;
        }
        int parkingDuration = Integer.parseInt(parkingDurationStr.substring(6).trim());

        return new Thread(new Car(gate, carId, arrivalTime, parkingDuration, parkingSpots, carsServed, gateCarCounts, logs));
    }

    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static class Car implements Runnable {
        private final String gate;
        private final int carId;
        private final int arrivalTime;
        private final int parkingDuration;
        private final Semaphore parkingSpots;
        private final AtomicInteger carsServed;
        private final Map<String, AtomicInteger> gateCarCounts;
        private final Queue<String> logs;

        public Car(String gate, int carId, int arrivalTime, int parkingDuration, Semaphore parkingSpots,
                   AtomicInteger carsServed, Map<String, AtomicInteger> gateCarCounts, Queue<String> logs) {
            this.gate = gate;
            this.carId = carId;
            this.arrivalTime = arrivalTime;
            this.parkingDuration = parkingDuration;
            this.parkingSpots = parkingSpots;
            this.carsServed = carsServed;
            this.gateCarCounts = gateCarCounts;
            this.logs = logs;
        }

        @Override
            public void run() {
                try {
                    Thread.sleep(arrivalTime * 1000);
                    logs.add("Car " + carId + " from " + gate + " arrived at time " + arrivalTime);
    
                    if (parkingSpots.tryAcquire()) {
                        try {
                            logs.add("Car " + carId + " from " + gate + " parked.");
                            gateCarCounts.get(gate).incrementAndGet();
                            carsServed.incrementAndGet();
    
                            Thread.sleep(parkingDuration * 1000);
                            logs.add("Car " + carId + " from " + gate + " left after " + parkingDuration + " units.");
                        } finally {
                            parkingSpots.release();
                        }
                    } else {
                        logs.add("Car " + carId + " from " + gate + " waiting for a spot.");
                        parkingSpots.acquire();
                        try {
                            logs.add("Car " + carId + " from " + gate + " parked after waiting.");
                            gateCarCounts.get(gate).incrementAndGet();
                            carsServed.incrementAndGet();
    
                            Thread.sleep(parkingDuration * 1000);
                            logs.add("Car " + carId + " from " + gate + " left after " + parkingDuration + " units.");
                        } finally {
                            parkingSpots.release();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }