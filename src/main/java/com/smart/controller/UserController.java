package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    // Add common data to response
    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String userName = principal.getName();
        User user = userRepository.getUserByUserName(userName);
        model.addAttribute("user", user);
    }

    // Dashboard home
    @RequestMapping("/index")
    public String dashboard(Model model) {
        model.addAttribute("title", "User Dashboard");
        return "normal/user_dashboard";
    }

    // Open add contact form
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model) {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    // Process add contact form
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                                 Principal principal) {

        try {
            String name = principal.getName();
            User user = userRepository.getUserByUserName(name);
            System.out.println("INSIDE 79"+contact);
            // Process and upload file
            if (file.isEmpty()) {
                contact.setImage("contact.png");
            } else {
                contact.setImage(file.getOriginalFilename());
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }

            user.getContacts().add(contact);
            contact.setUser(user);
            userRepository.save(user);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "normal/add_contact_form";
    }

 // show contacts handler
    @GetMapping("/show-contacts/{page}")
    public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {
        m.addAttribute("title", "Show User Contacts");

        // Fetch the user by their username (email)
        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);

        if (user != null) {
            // Setting up pagination and fetching contacts for the user
            Pageable pageable = PageRequest.of(page, 8);
            Page<Contact> contactsPage = this.contactRepository.findContactsByUser(user.getId(), pageable);

            // Logging for debugging
            System.out.println("User: " + user);
            System.out.println("Contacts Retrieved: " + contactsPage.getContent());

            m.addAttribute("contacts", contactsPage);
            m.addAttribute("currentPage", page);
            m.addAttribute("totalPages", contactsPage.getTotalPages());
        } else {
            System.out.println("User not found!");
        }

        return "normal/show_contacts";
    }


    // Show particular contact details
    @RequestMapping("/{cId}/contact")
    public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
        Optional<Contact> contactOptional = contactRepository.findById(cId);
        Contact contact = contactOptional.get();

        String userName = principal.getName();
        User user = userRepository.getUserByUserName(userName);

        if (user.getId() == contact.getUser().getId()) {
            model.addAttribute("contact", contact);
            model.addAttribute("title", contact.getName());
        }

        return "normal/contact_detail";
    }

    // Delete contact handler
    @GetMapping("/delete/{cid}")
    @Transactional
    public String deleteContact(@PathVariable("cid") Integer cId, Principal principal) {
        Contact contact = contactRepository.findById(cId).get();
        User user = userRepository.getUserByUserName(principal.getName());
        user.getContacts().remove(contact);
        userRepository.save(user);

        return "redirect:/user/show-contacts/0";
    }

    // Open update form handler
    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid, Model m) {
        m.addAttribute("title", "Update Contact");
        Contact contact = contactRepository.findById(cid).get();
        m.addAttribute("contact", contact);
        return "normal/update_form";
    }

    // Update contact handler
    @RequestMapping(value = "/process-update", method = RequestMethod.POST)
    public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                                Model m, Principal principal) {

        try {
            Contact oldContactDetail = contactRepository.findById(contact.getcId()).get();

            // Update image if a new file is provided
            if (!file.isEmpty()) {
                File deleteFile = new ClassPathResource("static/img").getFile();
                new File(deleteFile, oldContactDetail.getImage()).delete();

                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                contact.setImage(file.getOriginalFilename());
            } else {
                contact.setImage(oldContactDetail.getImage());
            }

            User user = userRepository.getUserByUserName(principal.getName());
            contact.setUser(user);
            contactRepository.save(contact);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/user/" + contact.getcId() + "/contact";
    }

    // Profile handler
    @GetMapping("/profile")
    public String yourProfile(Model model) {
        model.addAttribute("title", "Profile Page");
        return "normal/profile";
    }

    // Open settings handler
    @GetMapping("/settings")
    public String openSettings() {
        return "normal/settings";
    }

    // Change password handler
    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword, Principal principal) {

        String userName = principal.getName();
        User currentUser = userRepository.getUserByUserName(userName);

        if (bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
            currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
            userRepository.save(currentUser);
        } else {
            return "redirect:/user/settings";
        }

        return "redirect:/user/index";
    }
}
