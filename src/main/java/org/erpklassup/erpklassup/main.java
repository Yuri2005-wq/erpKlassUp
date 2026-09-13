package org.erpklassup.erpklassup;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.erpklassup.erpklassup.controllers.actionPage.LoginController;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.models.Ecole;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.PasswordService;
import org.erpklassup.erpklassup.service.SessionManager;
import static okhttp3.internal.Util.userAgent;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class main {
    public static void main(String[] args) {
        String mpd = "pass";
        PasswordService passwordService = new PasswordService();
        String var = passwordService.hacher(mpd);

        boolean estVrai = passwordService.verifier(mpd, " $2a$12$3hg4yjMILr3zCFL9cW8/eO4npSMHdJWTGZghoGl3TXM3AweELo3Rm");
        SessionManager session = SessionManager.getInstance();
        var text = session.getIdUtilisateurCourant();

        LoginController ctrl = new LoginController();
        String ip = ctrl.obtenirAdresseIpLocale();
        String t = userAgent;

        System.out.println(var);
        UtilisateurDAO dao = new UtilisateurDAO();
        Utilisateur user = dao.findByUsername("ton_username", "ton_id_ecole").orElseThrow();
        String secret = user.getSecret2FA();

        System.out.println("Secret : " + secret);

        // 2. Génère le code TOTP actuel
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        int code = gAuth.getTotpPassword(secret);

        System.out.println("Code actuel : " + String.format("%06d", code));
        System.out.println("(valide pendant 30 secondes)");
    }

}
