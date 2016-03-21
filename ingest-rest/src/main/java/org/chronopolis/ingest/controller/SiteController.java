package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.UserRequest;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.UserService;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.PasswordUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Controller for handling basic site interaction/administration
 *
 * Created by shake on 4/15/15.
 */
@Controller
public class SiteController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(SiteController.class);

    @Autowired
    UserDetailsManager manager;

    @Autowired
    NodeRepository repository;

    @Autowired
    UserService userService;

    /**
     * Get the index page
     *
     * @param model
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getIndex(Model model) {
        log.debug("GET index");
        return "index";
    }

    /**
     * Get the login page
     *
     * @return
     */
    @RequestMapping(value = "/login")
    public String login() {
        log.debug("LOGIN");
        return "login";
    }

    /**
     * Return a list of all users if called by an admin, otherwise only add the current
     * user
     *
     * @param model
     * @param principal
     * @return
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public String getUsers(Model model, Principal principal) {
        Collection<UserDetails> users = new ArrayList<>();
        Collection<Node> nodes = new ArrayList<>();
        String user = principal.getName();

        // Give admins a view into all users
        if (hasRoleAdmin()) {
            // TODO: This is pretty ugly but it lets us get all the users, except the admin...
            //       Maybe we could use the jdbctemplate and execute our own query instead
            //       since we're only doing a SELECT anyways
            for (Node node : repository.findAll()) {
                if (!node.getUsername().equals(user)) {
                    users.add(manager.loadUserByUsername(node.getUsername()));
                    nodes.add(node);
                }
            }

            // model.addAttribute("admin", true);
        }

        // Add the current user
        users.add(manager.loadUserByUsername(principal.getName()));
        nodes.add(repository.findByUsername(principal.getName()));

        model.addAttribute("users", users);

        return "users";
    }

    /**
     * Handle creation of a user
     * TODO: Make sure user does not exist before creating
     *
     * @param model
     * @param user
     * @return
     */
    @RequestMapping(value = "/users/add", method = RequestMethod.POST)
    public String createUser(Model model, UserRequest user) {
        log.debug("Request to create user: {} {} {}", new Object[]{user.getUsername(), user.isAdmin(), user.isNode()});
        userService.createUser(user);
        return "redirect:/users";
    }

    /**
     * Handler for updating the current users password
     *
     * @param model
     * @param update
     * @return
     */
    @RequestMapping(value = "/users/update", method = RequestMethod.POST)
    public String updateUser(Model model, PasswordUpdate update) {
        manager.changePassword(update.getOldPassword(), update.getNewPassword());
        return "redirect:/users";
    }

}
