package org.erpklassup.erpklassup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.erpklassup.erpklassup.dao.GRH.DepartementDAO;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.service.PasswordService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.sync.SyncApiClient;

import java.util.List;
import java.util.UUID;

public class main {
    public static void main(String[] args) throws Exception {
        PasswordService passwordService = new PasswordService();
        String pass = "pass";
        String mpd =passwordService.hacher(pass);
        System.out.println(mpd);


//        DepartementDAO departementDAO = new DepartementDAO();
//        Departement d = new Departement("pen", "ECO_001");
//        try{
//            departementDAO.create(d);
//        }catch(Exception e){
//            System.err.println("Erreur" + e.getMessage());
//        }
//        System.out.println(departementDAO.nomDepartementExist("b05d34ae-68d4-4fc9-bbbd-599fb046ad7","pen"));
    }
}