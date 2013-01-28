package org.openlmis.core.service;

import lombok.NoArgsConstructor;
import org.openlmis.core.domain.User;
import org.openlmis.core.exception.DataException;
import org.openlmis.core.repository.UserRepository;
import org.openlmis.email.domain.EmailMessage;
import org.openlmis.email.exception.EmailException;
import org.openlmis.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@NoArgsConstructor
public class UserService {

  public static final String USER_EMAIL_NOT_FOUND = "user.email.not.found";
  public static final String USER_EMAIL_INCORRECT = "user.email.incorrect";
  private static final String USER_USERNAME_INCORRECT = "user.username.incorrect";

  @Value("${accountcreated.email.subject}")
  private String ACCOUNT_CREATED_EMAIL_SUBJECT;

  @Value("${passwordreset.email.body}")
  private String PASSWORD_RESET_CREATED_EMAIL_BODY;

  @Value("${forgotpassword.email.subject}")
  private String FORGOT_PASSWORD_EMAIL_SUBJECT;

  private UserRepository userRepository;

  private EmailService emailService;

  private RoleAssignmentService roleAssignmentService;

  @Autowired
  public UserService(UserRepository userRepository, RoleAssignmentService roleAssignmentService, EmailService emailService) {
    this.userRepository = userRepository;
    this.emailService = emailService;
    this.roleAssignmentService = roleAssignmentService;
  }

  public void save(User user) {
    user.validate();
    Boolean createFlag = user.getId() == null;
    userRepository.insert(user);

    if (!createFlag) {
      roleAssignmentService.deleteAllRoleAssignmentsForUser(user.getId());
    }

    roleAssignmentService.insertUserProgramRoleMapping(user, user.getProgramToRoleMappingList());

    if (createFlag) {
      EmailMessage emailMessage = accountCreatedEmailMessage(user.getEmail());
      sendEmail(emailMessage);
    }
  }

  private void sendEmail(EmailMessage emailMessage) {
    try {
      emailService.send(emailMessage);
    } catch (EmailException e) {
      throw new DataException(USER_EMAIL_NOT_FOUND);
    }
  }

  public void sendForgotPasswordEmail(User user) {
    user = getValidatedUser(user);
    EmailMessage emailMessage = forgotPasswordEmailMessage(user.getEmail());
    emailMessage.setTo(user.getEmail());
    sendEmail(emailMessage);
  }

  private User getValidatedUser(User user) {
    if (user.getEmail() != null && !user.getEmail().equals("")) {
      user = userRepository.getByEmail(user.getEmail());
      if (user == null) throw new DataException(USER_EMAIL_INCORRECT);
    } else {
      user = userRepository.getByUsername(user.getUserName());
      if (user == null) throw new DataException(USER_USERNAME_INCORRECT);
    }
    return user;
  }

  private EmailMessage createEmailMessage(String email) {
    EmailMessage emailMessage = new EmailMessage();
    emailMessage.setTo(email);
    emailMessage.setText(PASSWORD_RESET_CREATED_EMAIL_BODY + generateUUID());
    return emailMessage;
  }

  private EmailMessage accountCreatedEmailMessage(String email) {
    EmailMessage emailMessage = createEmailMessage(email);
    emailMessage.setSubject(ACCOUNT_CREATED_EMAIL_SUBJECT);
    return emailMessage;
  }

  private EmailMessage forgotPasswordEmailMessage(String email) {
    EmailMessage emailMessage = createEmailMessage(email);
    emailMessage.setSubject(FORGOT_PASSWORD_EMAIL_SUBJECT);
    return emailMessage;
  }

  private String generateUUID() {
    return UUID.randomUUID().toString();
  }

  public List<User> searchUser(String userSearchParam) {
    return userRepository.searchUser(userSearchParam);
  }

  public User getById(Integer id) {
    User user = userRepository.getById(id);
    user.setProgramToRoleMappingList(roleAssignmentService.getListOfProgramToRoleMappingForAUser(id));
    return user;
  }
}
