package com.uber.cadence;

import static com.uber.cadence.testUtils.TestEnvironment.DOMAIN;
import static com.uber.cadence.testUtils.TestEnvironment.DOMAIN2;

import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testUtils.TestEnvironment;

/** Waits for local service to become available and registers UnitTest domain. */
public class RegisterTestDomain {
  private static final boolean useDockerService = TestEnvironment.isUseDockerService();

  public static void main(String[] args) throws InterruptedException {
    if (!useDockerService) {
      return;
    }

    IWorkflowService service = TestEnvironment.getDockerService();
    registerDomain(service, DOMAIN);
    registerDomain(service, DOMAIN2);
    System.exit(0);
  }

  private static void registerDomain(IWorkflowService service, String domain)
      throws InterruptedException {
    RegisterDomainRequest request =
        new RegisterDomainRequest().setName(domain).setWorkflowExecutionRetentionPeriodInDays(1);
    while (true) {
      try {
        service.RegisterDomain(request);
        break;
      } catch (DomainAlreadyExistsError e) {
        break;
      } catch (BaseError e) {
        String message = e.getMessage();
        if (message != null
            && !message.contains("Failed to connect to the host")
            && !message.contains("Connection timeout on identification")) {
          e.printStackTrace();
        }
        Thread.sleep(500);
        continue;
      } catch (Throwable e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }
}
