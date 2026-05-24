package emu.nebula.util;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final String MALL_COUNTER_DOT_ESCAPE = "__dot__";
    
    public static final Object EMPTY_OBJECT = new Object();
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, 0);
    }
    
    public static String bytesToHex(byte[] bytes, int offset) {
        if (bytes == null || (bytes.length - offset) <= 0) return "";
        char[] hexChars = new char[(bytes.length - offset) * 2];
        for (int j = offset; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            int h = j - offset;
            hexChars[h * 2] = HEX_ARRAY[v >>> 4];
            hexChars[h * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String capitalize(String s) {
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    public static String lowerCaseFirstChar(String s) {
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    public static String escapeKey(String key) {
        if (key == null || key.indexOf('.') < 0) {
            return key;
        }

        return key.replace(".", MALL_COUNTER_DOT_ESCAPE);
    }

    public static String unescapeKey(String key) {
        if (key == null || !key.contains(MALL_COUNTER_DOT_ESCAPE)) {
            return key;
        }

        return key.replace(MALL_COUNTER_DOT_ESCAPE, ".");
    }

    /**
     * Creates a string with the path to a file.
     * @param path The path to the file.
     * @return A path using the operating system's file separator.
     */
    public static String toFilePath(String path) {
        return path.replace("/", File.separator);
    }

    /**
     * Checks if a file exists on the file system.
     * @param path The path to the file.
     * @return True if the file exists, false otherwise.
     */
    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    /**
     * Creates a folder on the file system.
     * @param path The path to the folder.
     * @return True if the folder was created, false otherwise.
     */
    public static boolean createFolder(String path) {
        return new File(path).mkdirs();
    }

    public static int getMinAdvanceForLevel(int level) {
        return Math.max(Math.min((int) ((level - 1) / 10D), 8), 0);
    }

    /**
     * Parses the string argument as a signed decimal integer. Returns a 0 if the string argument is not an integer.
     */
    public static int parseSafeInt(String s) {
        if (s == null) {
            return 0;
        }

        int i = 0;

        try {
            i = Integer.parseInt(s);
        } catch (Exception e) {
            i = 0;
        }

        return i;
    }

    /**
     * Parses the string argument as a signed decimal long. Returns a 0 if the string argument is not a long.
     */
    public static long parseSafeLong(String s) {
        if (s == null) {
            return 0;
        }

        long i = 0;

        try {
            i = Long.parseLong(s);
        } catch (Exception e) {
            i = 0;
        }

        return i;
    }
    
    /**
     * Add 2 integers without overflowing
     */
    public static int safeAdd(int a, int b) {
        return safeAdd(a, b, Integer.MAX_VALUE, 0);
    }
    
    public static int safeAdd(int a, int b, long max, long min) {
        long sum = (long) a + (long) b;
        
        if (sum > max) {
            return (int) max;
        } else if (sum < min) {
            return (int) min;
        }
        
        return (int) sum;
    }
    
    /**
     * Subtract 2 integers without overflowing
     */
    public static int safeSubtract(int a, int b) {
        return safeSubtract(a, b, Integer.MAX_VALUE, Integer.MIN_VALUE);
    }
    
    public static int safeSubtract(int a, int b, long max, long min) {
        long sum = (long) a - (long) b;
        
        if (sum > max) {
            return (int) max;
        } else if (sum < min) {
            return (int) min;
        }
        
        return (int) sum;
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
    
    public static double generateRandomDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public static int randomRange(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    public static boolean randomChance(double chance) {
        if (chance <= 0D) {
            return false;
        }
        
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public static int randomElement(int[] array) {
        return array[ThreadLocalRandom.current().nextInt(0, array.length)];
    }

    public static <T> T randomElement(List<T> list) {
        return randomElement(list, false);
    }
    
    public static <T> T randomElement(List<T> list, boolean remove) {
        int index = ThreadLocalRandom.current().nextInt(0, list.size());
        var object = list.get(index);
        
        if (remove) {
            list.remove(index);
        }
        
        return object;
    }
    
    public static int randomElement(IntList list) {
        return list.getInt(ThreadLocalRandom.current().nextInt(0, list.size()));
    }
    
    public static int randomElement(IntList list, boolean remove) {
        int index = ThreadLocalRandom.current().nextInt(0, list.size());
        int object = list.getInt(index);
        
        if (remove) {
            list.removeInt(index);
        }
        
        return object;
    }

    /**
     * Checks if an integer array contains a value
     * @param array
     * @param value The value to check for
     */
    public static boolean arrayContains(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) return true;
        }
        return false;
    }

    public static boolean arrayContains(Integer[] array, int value) {
        for (Integer element : array) {
            if (element != null && element.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean arrayContains(List<Integer> list, int value) {
        for (Integer element : list) {
            if (element.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static int[] convertListToIntArray(List<Integer> list) {
        // Create an int array with the same size as the list
        int[] intArray = new int[list.size()];

        // Iterate over the list and populate the int array
        for (int i = 0; i < list.size(); i++) {
            intArray[i] = list.get(i);
        }

        return intArray;
    }

    /**
     * Base64 encodes a given byte array.
     * @param toEncode An array of bytes.
     * @return A base64 encoded string.
     */
    public static String base64Encode(byte[] toEncode) {
        return Base64.getEncoder().encodeToString(toEncode);
    }

    /**
     * Base64 decodes a given string.
     * @param toDecode A base64 encoded string.
     * @return An array of bytes.
     */
    public static byte[] base64Decode(String toDecode) {
        return Base64.getDecoder().decode(toDecode);
    }

    /**
     * Checks if a port is open on a given host.
     *
     * @param host The host to check.
     * @param port The port to check.
     * @return True if the port is open, false otherwise.
     */
    public static boolean isPortOpen(String host, int port) {
        try (var serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName(host), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Get amount of weeks since this epoch day. Each week starts on monday.
     * @param epochDays
     * @return
     */
    public static int getWeeks(long epochDays) {
        return (int) Math.floor((epochDays + 3) / 7D);
    }
    
    /**
     * Get amount of months since this epoch day.
     * @param epochDays
     * @return
     */
    public static int getMonths(long epochDays) {
        var begin = LocalDate.ofEpochDay(0);
        var date = LocalDate.ofEpochDay(epochDays);
        return (int) ChronoUnit.MONTHS.between(begin, date);
    }
    
    public static int getDaysOfMonth(long epochDays) {
        var date = LocalDate.ofEpochDay(epochDays);
        var month = YearMonth.of(date.getYear(), date.getMonthValue());
        return month.lengthOfMonth();
    }

    public static String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(GameConstants.UTC_ZONE)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String formatTimestamp(long timestamp, String pattern) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(GameConstants.UTC_ZONE)
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    public static long dateToMilliseconds(String dateString) {
        var offsetDateTime = OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return offsetDateTime.toInstant().toEpochMilli();
    }

    /**
     * Convert ISO offset datetime to epoch seconds, return 0 on empty/invalid
     */
    public static long dateToSeconds(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return 0L;
        }

        try {
            return dateToMilliseconds(dateString) / 1000L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public static long getNextResetTimeSeconds(int refreshTimeType) {
        ResetCycle resetCycle = ResetCycle.fromRefreshType(refreshTimeType);
        return getNextResetTimeSeconds(resetCycle);
    }

    public static long getNextResetTimeSeconds(ResetCycle resetCycle) {
        if (resetCycle == null) {
            return 0;
        }
        return getNextResetTimeSeconds(resetCycle, Nebula.getCurrentServerTime());
    }

    public static long getNextResetTimeSeconds(ResetCycle cycle, long nowSeconds) {
        long resetHour = Nebula.getConfig().getServerOptions().getDailyResetHour();
        LocalDate shiftedDate = getShiftedResetDate(nowSeconds, resetHour);
        LocalDate nextResetDate = switch (cycle) {
            case DAILY -> shiftedDate.plusDays(1);
            case WEEKLY -> shiftedDate.plusDays(8L - shiftedDate.getDayOfWeek().getValue());
            case MONTHLY -> shiftedDate.withDayOfMonth(1).plusMonths(1);
        };

        return toResetEpochSecond(nextResetDate, resetHour);
    }

    public static long getResetEpochDay() {
        return getResetEpochDay(Nebula.getCurrentServerTime());
    }

    public static long getResetEpochDay(long nowSeconds) {
        return getShiftedResetDate(nowSeconds, Nebula.getConfig().getServerOptions().getDailyResetHour()).toEpochDay();
    }

    /**
     * Convert epochDay to server local time seconds.
     */
    public static long getResetTimeSecondsByEpochDay(long epochDay) {
        return toResetEpochSecond(LocalDate.ofEpochDay(epochDay), Nebula.getConfig().getServerOptions().getDailyResetHour());
    }

    public static int getResetPeriodIndex(ResetCycle cycle, long nowSeconds) {
        long epochDay = getResetEpochDay(nowSeconds);
        return switch (cycle) {
            case DAILY -> Math.toIntExact(epochDay);
            case WEEKLY -> getWeeks(epochDay);
            case MONTHLY -> getMonths(epochDay);
        };
    }

    private static LocalDate getShiftedResetDate(long nowSeconds, long resetHour) {
        return LocalDate.ofInstant(
                Instant.ofEpochSecond(nowSeconds).minusSeconds(resetHour * 3600L),
                getResetZone()
        );
    }

    private static long toResetEpochSecond(LocalDate date, long resetHour) {
        return date.atStartOfDay(getResetZone()).toEpochSecond() + (resetHour * 3600L);
    }

    /**
     * Server local timezone as reset timezone
     */
    private static ZoneId getResetZone() {
        return ZoneId.systemDefault();
    }

}
