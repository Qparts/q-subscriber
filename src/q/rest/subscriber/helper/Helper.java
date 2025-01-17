package q.rest.subscriber.helper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class Helper {

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";

    private static final String SALT = CHAR_LOWER + CHAR_UPPER + NUMBER;



    public static int getSubscriberFromJWT(String header) {
        String token = header.substring("Bearer".length()).trim();
        Claims claims = Jwts.parserBuilder().setSigningKey(KeyConstant.PUBLIC_KEY).build().parseClaimsJws(token).getBody();
        return Integer.parseInt(claims.get("sub").toString());
    }


    public static int getCompanyFromJWT(String header) {
        String token = header.substring("Bearer".length()).trim();
        Claims claims = Jwts.parserBuilder().setSigningKey(KeyConstant.PUBLIC_KEY).build().parseClaimsJws(token).getBody();
        return Integer.parseInt(claims.get("comp").toString());
    }

    public List<Date> getAllDatesBetween(Date from, Date to, boolean excludeFriday){
        from = new Date(from.getTime());
        to = new Date(to.getTime() + (1000*60*60*24));
        LocalDate fromLocal = convertToLocalDate(from);
        LocalDate toLocal = convertToLocalDate(to);
        List<LocalDate> localDates;
        if(excludeFriday){
            Set<DayOfWeek> fridays = EnumSet.of(DayOfWeek.FRIDAY);
            localDates = fromLocal.datesUntil(toLocal).filter(d -> !fridays.contains(d.getDayOfWeek()))
                    .collect(Collectors.toList());
        }
        else{
            localDates = fromLocal.datesUntil(toLocal).collect(Collectors.toList());
        }
        List<Date> dates = new ArrayList<>();
        for(LocalDate ld : localDates){
            dates.add(convertToDate(ld));
        }
        return dates;
    }


    public static Date getToDate(int month, int year) {
        YearMonth ym = YearMonth.of(year,month);
        LocalDate to = ym.atEndOfMonth();
        return convertToDate(to);
    }

    public static Date getFromDate(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        return convertToDate(from);
    }



    public static LocalDate convertToLocalDate(Date dateToConvert) {
        return LocalDate.ofInstant(
                dateToConvert.toInstant(), ZoneId.systemDefault());
    }

    public static Date convertToDate(LocalDate dateToConvert) {
        return Date.from(dateToConvert.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    public static String getRandomString(int length) {
        SecureRandom random = new SecureRandom();
        if (length < 1) throw new IllegalArgumentException();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {

            // 0-62 (exclusive), random returns 0-61
            int rndCharAt = random.nextInt(SALT.length());
            char rndChar = SALT.charAt(rndCharAt);
            sb.append(rndChar);
        }
        return sb.toString();
    }

    public static String undecorate(String string) {
        return string.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    public static String getSecuredRandom() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    public static String getSecuredRandom(int length) {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(length);
    }


    public static String getFullMobile(String mobile, String countryCode){
        String mobileFull = mobile;
        mobileFull = mobileFull.replaceFirst("^0+(?!$)", "");
        mobileFull = countryCode + mobileFull;
        return mobileFull;
    }



    public static String cypher(String text) {
        try {
            String shaval = "";
            MessageDigest algorithm = MessageDigest.getInstance("SHA-256");

            byte[] defaultBytes = text.getBytes();

            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            shaval = hexString.toString();

            return shaval;
        }catch (Exception ex){
            return text;
        }
    }

    public static int getRandomInteger(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    public static Date addSeconds(Date original, int seconds) {
        return new Date(original.getTime() + (1000L * seconds));
    }

    public static Date addMinutes(Date original, int minutes) {
        return new Date(original.getTime() + (1000L * 60 * minutes));
    }

    public static Date addDays(Date original, long days) {
        return new Date(original.getTime() + (1000L * 60 * 60 * 24 * days));
    }

    public String getDateFormat(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");
        return sdf.format(date);
    }

    public String getDateFormat(Date date, String pattern){
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    public String getCurrencyFormat(double amount){
        return "SR "+ new DecimalFormat("#.##").format(amount);
    }

    public static int parseId(String query){
        try{
            return Integer.parseInt(query);
        }catch (Exception ex){
            return 0;
        }
    }

}
