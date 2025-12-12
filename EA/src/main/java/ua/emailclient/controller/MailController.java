package ua.emailclient.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.emailclient.model.Mail;
import ua.emailclient.service.IMailService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.List;

@Controller
@RequestMapping("/mail")
public class MailController {

    private final IMailService mailService;

    public MailController(IMailService mailService) {
        this.mailService = mailService;
    }

    private String getUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser) return ((OidcUser) principal).getEmail();
        if (principal instanceof OAuth2User) return ((OAuth2User) principal).getAttribute("email");
        return authentication.getName();
    }

    @GetMapping("/new")
    public String newMailForm(Model model, Authentication authentication) {
        Mail newMail = new Mail();
        newMail.setSender(getUsername(authentication));
        model.addAttribute("mail", newMail);
        return "letter";
    }

    @GetMapping("/drafts/{id}")
    public String editDraftForm(@PathVariable("id") String id, Model model, Authentication authentication) {
        Mail draft = mailService.getDraftById(id, getUsername(authentication));
        if (draft == null) return "redirect:/home";

        model.addAttribute("mail", draft);
        return "letter";
    }

    @PostMapping("/saveDraft")
    public String saveDraft(@ModelAttribute Mail mail, Authentication authentication, RedirectAttributes ra) {
        try {
            Mail saved = mailService.saveDraft(mail, getUsername(authentication));
            ra.addFlashAttribute("message", "Чернетку збережено!");
            return "redirect:/mail/drafts/" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Помилка: " + e.getMessage());
            return "redirect:/mail/new";
        }
    }

    @PostMapping("/send")
    public String sendMail(@ModelAttribute Mail mail, Authentication authentication, RedirectAttributes ra) {
        try {
            mailService.sendMail(mail, getUsername(authentication));
            ra.addFlashAttribute("message", "Лист відправлено!");
            return "redirect:/home";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Помилка відправки: " + e.getMessage());
            return "redirect:/mail/new";
        }
    }

    @GetMapping("/inbox")
    public String viewInbox(Model model, Authentication auth) {
        model.addAttribute("mails", mailService.getInbox(getUsername(auth)));
        model.addAttribute("folderName", "Вхідні");
        model.addAttribute("folderType", "inbox");
        return "mailbox";
    }

    @GetMapping("/sent")
    public String viewSent(Model model, Authentication auth) {
        model.addAttribute("mails", mailService.getSent(getUsername(auth)));
        model.addAttribute("folderName", "Відправлені");
        model.addAttribute("folderType", "sent");
        return "mailbox";
    }

    @GetMapping("/drafts")
    public String viewDrafts(Model model, Authentication auth) {
        model.addAttribute("mails", mailService.getDrafts(getUsername(auth)));
        model.addAttribute("folderName", "Чернетки");
        model.addAttribute("folderType", "drafts");
        return "mailbox";
    }

    @GetMapping("/{id}")
    public String readMail(@PathVariable("id") String id, Model model, Authentication auth) {
        Mail mail = mailService.getMailById(id, getUsername(auth));
        if (mail == null) return "redirect:/home";
        model.addAttribute("mail", mail);
        return "read";
    }
}