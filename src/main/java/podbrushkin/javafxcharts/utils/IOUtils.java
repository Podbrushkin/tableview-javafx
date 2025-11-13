package podbrushkin.javafxcharts.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IOUtils {
    public static Map<String, String> parseArgs(List<String> args) {
        return parseArgs(args.toArray(new String[0]));
    }
    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            boolean isNamed = args[i].startsWith("--");
            var values = List.of(args).stream().skip(i+1).takeWhile(v -> !v.startsWith("--")).collect(Collectors.toList());
            
            // Common <--key value1...>
            if (isNamed && values.size() >= 1) {
                result.put(args[i].substring(2), String.join(";", values));
                i += values.size(); continue;
            }
            // Switch <--key>
            else if (isNamed && values.size() == 0)
                result.put(args[i].substring(2), ""); // switch true
            else System.err.println("What is this arg? "+args[i]);
        }
        return result;
    }
    
}
