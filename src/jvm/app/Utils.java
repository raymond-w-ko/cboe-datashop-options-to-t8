package app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;

public final class Utils {

  private Utils() {
  }

  public static int ExpDateToInt(String s) {
    final var year = Integer.parseInt(s.substring(0, 4));
    final var month = Integer.parseInt(s.substring(5, 7));
    final var day = Integer.parseInt(s.substring(8, 10));
    return day + (100 * month) + (10000 * year);
  }

  public static String IntToExpDate(int i) {
    final String day = StringUtils.leftPad(String.valueOf(i % 100), 2, "0");
    i = i / 100;
    final String month = StringUtils.leftPad(String.valueOf(i % 100), 2, "0");
    i = i / 100;
    final String year = String.valueOf(i);
    return year + "-" + month + "-" + day;
  }

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss");
  private static ZoneId NewYorkZoneId = ZoneId.of("America/New_York");
  private static ZoneId UTCZoneId = ZoneId.of("UTC");
  private static LocalDateTime OAStartLocalDateTime = LocalDateTime.parse("1899-12-30 00:00:00", dateTimeFormatter);
  private static ZonedDateTime OAStartZonedDateTime = ZonedDateTime.of(OAStartLocalDateTime, UTCZoneId);

  private static int CACHE_MAX_SIZE = 4 * 1024 * 1024;
  private static ConcurrentHashMap<String, Double> OADateLookupMap = new ConcurrentHashMap<String, Double>(
      CACHE_MAX_SIZE);

  /**
   * Convert timestamp to Microsoft OLE Automation - OADate type
   * A timestamp is in the format of "2006-12-01 09:31:00"
   * 
   * @param timestamp
   * @return
   * @throws ParseException
   */
  public static Double convertQuoteDateTimeToOADate(String timestamp) throws ParseException {
    if (OADateLookupMap.containsKey(timestamp)) {
      return OADateLookupMap.get(timestamp);
    }
    if (OADateLookupMap.size() > CACHE_MAX_SIZE) {
      System.out.println("OADateLookupMap clear()");
      OADateLookupMap.clear();
    }
    final var ldt = LocalDateTime.parse(timestamp, dateTimeFormatter);
    final var zdt = ZonedDateTime.of(ldt, NewYorkZoneId);
    final var utcZdt = zdt.withZoneSameInstant(UTCZoneId);

    final var duration = Duration.between(OAStartZonedDateTime, utcZdt);

    double oaDate = duration.toMillis() / (double) TimeUnit.DAYS.toMillis(1);
    OADateLookupMap.put(timestamp, oaDate);
    return oaDate;
  }

  private static ConcurrentHashMap<String, Instant> instantLookupMap = new ConcurrentHashMap<String, Instant>(
    CACHE_MAX_SIZE
  );

  public static Instant QuoteDateTimeToInstant(String s) {
    if (instantLookupMap.containsKey(s)) {
      return instantLookupMap.get(s);
    }
    if (instantLookupMap.size() > CACHE_MAX_SIZE) {
      System.out.println("instantLookupMap clear()");
      instantLookupMap.clear();
    }

    final var ldt = LocalDateTime.parse(s, dateTimeFormatter);
    final var zdt = ZonedDateTime.of(ldt, NewYorkZoneId);
    final var t = zdt.toInstant();
    instantLookupMap.put(s, t);
    return t;
  }

  private static ConcurrentHashMap<String, Integer> DTELookupMap = new ConcurrentHashMap<String, Integer>(
    CACHE_MAX_SIZE
  );

  public static Integer DTE(String quoteDateTime, String expDate) {
    String key = quoteDateTime + "\n" + expDate;
    if (DTELookupMap.containsKey(key)) {
      return DTELookupMap.get(key);
    }
    final var quoteInstant = QuoteDateTimeToInstant(quoteDateTime);
    final var expInstant = QuoteDateTimeToInstant(expDate + " 23:59:59");
    final var duration = Duration.between(quoteInstant, expInstant);
    Integer dte = (int) duration.toDays();
    DTELookupMap.put(key, dte);
    return dte;
  }
}
