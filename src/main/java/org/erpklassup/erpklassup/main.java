package org.erpklassup.erpklassup;

import org.erpklassup.erpklassup.service.PasswordService;

public class main {
    public static void main (String[] args){
        String mpd = "pass";
        PasswordService passwordService = new PasswordService();
        String var = passwordService.hacher(mpd);

        boolean estVrai = passwordService.verifier(mpd, "$2a$12$3hg4yjMILr3zCFL9cW8/eO4npSMHdJWTGZghoGl3TXM3AweELo3Rm");

        System.out.println(estVrai);
    }
}
