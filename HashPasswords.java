import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.*;

public class HashPasswords {
    public static void main(String[] args) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        
        String[] passwords = {
            "Smo1001", "Smo1002", "Smo1003", "Smo1004", "Smo1005", "Smo1006", "Smo1007", "Smo1008", "Smo1009", "Smo1010",
            "Smo1011", "Smo1012", "Smo1013", "Smo1014", "Smo1015", "Smo1016", "Smo1017", "Smo1018", "Smo1019", "Smo1020",
            "Smo1021", "Smo1022", "Smo1023", "Smo1024", "Smo1025", "Smo1026", "Smo1027", "Smo1028", "Smo1029", "Smo1030",
            "Smo1031", "Smo1032", "Smo1033", "Smo1034", "Smo1035", "Smo1036", "Smo1037", "Smo1038", "Smo1039", "Smo1040",
            "Smo1041", "Smo1042", "Smo1043", "Smo1046", "Smo1045", "smo1047", "smo1048", "Smo1049", "Smo1050", "Smo1051",
            "Smo1052", "Smo1053", "Smo1054", "Smo1055", "Smo1056", "Smo1057", "Smo1058", "Smo1059", "Smo1060", "test123"
        };
        
        System.out.println("-- BCrypt hashed passwords for SMO employees");
        for (int i = 0; i < passwords.length; i++) {
            String hashed = encoder.encode(passwords[i]);
            System.out.println("(" + (1001 + i) + ", '" + hashed + "'),");
        }
    }
}
