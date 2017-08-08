package fsbtracking;

import com.opencsv.*;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import org.apache.commons.lang3.tuple.*;

public class FSBTracking {

    static final String fileName = "FSB.csv";
    static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");

    static final class Entry {

        public final int day, month;
        public final Date date;
        public final double price;

        public Entry(Date date, Double price) {
            this.date = date;
            this.price = price;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            day = calendar.get(Calendar.DAY_OF_WEEK);
            month = calendar.get(Calendar.MONTH);
        }
    }

    public static void main(String[] args) throws Exception {
        CSVReader fin = new CSVReader(new FileReader(fileName));
        List<String[]> hist = fin.readAll();
        List<Entry> prices = new ArrayList();
        List<MutableTriple<Double, Date, Double>> dailyRet = new ArrayList(), weeklyRet = new ArrayList(), monthlyRet = new ArrayList();//return, date, ratio
        for (String[] itr : hist) {
            Date cobDate = sdf.parse(itr[0]);
            double price = Double.parseDouble(itr[1]);
            prices.add(new Entry(cobDate, price));
        }
        double lastWeekEnd = prices.get(prices.size() - 1).price;
        double lastMonthEnd = lastWeekEnd;
        for (int i = prices.size() - 2; i >= 0; i--) {
            if (i == 0 || prices.get(i - 1).day < prices.get(i).day) {
                double ret = (prices.get(i).price / lastWeekEnd - 1) * 100;
                weeklyRet.add(new MutableTriple(ret, prices.get(i).date, 0));
                lastWeekEnd = prices.get(i).price;
            }
            
            if (i == 0 || prices.get(i - 1).month != prices.get(i).month) {
                double ret = (prices.get(i).price / lastMonthEnd - 1) * 100;
                monthlyRet.add(new MutableTriple(ret, prices.get(i).date, 0));
                lastMonthEnd = prices.get(i).price;
            }

            if (prices.get(i).day > prices.get(i + 1).day) {
                double ret = (prices.get(i).price / prices.get(i + 1).price - 1) * 100;
                dailyRet.add(new MutableTriple(ret, prices.get(i).date, 0));
            }
        }
        Collections.reverse(monthlyRet);
        Collections.reverse(weeklyRet);
        Collections.reverse(dailyRet);
        
        double recentWeeklyRet = weeklyRet.get(0).left;
        weeklyRet.remove(0);
        System.out.printf("Most recent weekly return: %.5f%%%n", recentWeeklyRet);
        fillRatio(weeklyRet, 0.97);
        Pair<Double, Double> stats = meanAndStdev(weeklyRet);
        System.out.printf("Mean and stdev of weekly return : %.5f%%, %.5f%%%n", stats.getLeft(), stats.getRight());
        double pValue = pValue(weeklyRet, recentWeeklyRet);
        System.out.printf("pValue of most recent weekly return : %.5f%%%n%n%n", pValue * 100);

        double recentDailyRet = dailyRet.get(0).left;
        dailyRet.remove(0);
        System.out.printf("Most recent daily return: %.5f%%%n", recentDailyRet);
        fillRatio(dailyRet, 0.99);
        stats = meanAndStdev(dailyRet);
        System.out.printf("Mean and stdev of daily return : %.5f%%, %.5f%%%n", stats.getLeft(), stats.getRight());
        pValue = pValue(dailyRet, recentDailyRet);
        System.out.printf("pValue of most recent daily return : %.5f%%%n%n%n", pValue * 100);
        
        monthlyRet.remove(0);
        fillRatio(monthlyRet, 0.95);
        stats = meanAndStdev(monthlyRet);
        System.out.printf("%.5f%% \t %.5f%% \t %.5f \t %.5f%%%n", stats.getLeft(), stats.getRight(), stats.getLeft()/stats.getRight()*Math.sqrt(12.0), VaR(monthlyRet, 0.95));
    }

    static void fillRatio(List<MutableTriple<Double, Date, Double>> hist, double lambda) {
        if (lambda == 1.0) {
            hist.get(0).right = 1.0 / hist.size();
        } else {
            hist.get(0).right = (1 - lambda) / (1 - Math.pow(lambda, hist.size()));
        }
        for (int i = 1; i < hist.size(); i++) {
            hist.get(i).right = hist.get(i - 1).right * lambda;
        }
    }

    static Pair<Double, Double> meanAndStdev(List<MutableTriple<Double, Date, Double>> hist) {
        double mean = 0;
        for (MutableTriple<Double, Date, Double> itr : hist) {
            mean += itr.left * itr.right;
        }
        double varience = 0;
        for (MutableTriple<Double, Date, Double> itr : hist) {
            varience += (itr.left - mean) * (itr.left - mean) * itr.right;
        }
        varience = varience * (hist.size()) / (hist.size() - 1);
        return new ImmutablePair(mean, Math.sqrt(varience));
    }

    static double pValue(List<MutableTriple<Double, Date, Double>> hist, double ret) {
        Collections.sort(hist);
        double pValue = 0;
        for (int i = 0; i < hist.size() && hist.get(i).left <= ret; i++) {
            pValue += hist.get(i).right;
        }
        return pValue;
    }
    
    static double VaR(List<MutableTriple<Double, Date, Double>> hist, double p) {
        Collections.sort(hist);
        double pValue = 0;
        int i;
        for (i = 0; i< hist.size() && pValue < 1-p; i++) {
            pValue += hist.get(i).right;
        }
        return hist.get(i).left;
    }
}
