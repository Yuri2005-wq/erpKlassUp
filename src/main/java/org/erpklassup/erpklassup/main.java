package org.erpklassup.erpklassup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.erpklassup.erpklassup.dao.GRH.DepartementDAO;
import org.erpklassup.erpklassup.dao.PersonnelDAO;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.models.GRH.Personnel;
import org.erpklassup.erpklassup.service.PasswordService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.sync.SyncApiClient;
import org.erpklassup.erpklassup.util.PersonnelUtils;

import java.util.List;
import java.util.UUID;

public class main {
    public static void main(String[] args) throws Exception {


        PersonnelUtils personnelUtils = new PersonnelUtils();


        String test = PersonnelUtils.genererMatriculeEleve("ECO_001");

        System.out.println(test);


//        PasswordService passwordService = new PasswordService();
//        String pass = "pass";
//        String mpd =passwordService.hacher(pass);
//        System.out.println(mpd);


//        PersonnelDAO personnelDAO = new PersonnelDAO();
//
//        Personnel personnel = new Personnel();
//
//        personnel.setId(" PERS_PHOTO_01");
//        personnel.setNom("Ngongang Djaleu");
//        personnel.setPrenom("Krist Francelle");
//        personnel.setVersion(1L);
//
//
//        personnelDAO.findById(" PERS_PHOTO_01");
//
////
////        personnel.setIdPersonnel(personnel.getIdPersonnel());
////        personnel.setIdEcole("ECO_001");
////        personnel.setMatriculeInterne("FIS20271163");
////        personnel.setTypePersonnel("ENSEIGNANT");
////        personnel.setPosteOuFonction("Enseignant");
////        personnel.setNom("Ngongang Djaleu");
////        personnel.setPrenom("Krist Francelle");
////        personnel.setSexe("Féminin");
////        personnel.setNumeroMobileMoney("691371808");
//
//
////        personnelDAO.insert(personnel);
//
//
//
    }
}