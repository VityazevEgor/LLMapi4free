package com.vityazev_egor.LLMs;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.Core.LambdaWaitTask;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.iChat;
import com.vityazev_egor.Models.ChatAnswer;

public class DeepSeek implements iChat{
    private final NoDriver driver;
    private final String url = "https://chat.deepseek.com/";
    private final CustomLogger logger = new CustomLogger(DeepSeek.class.getName());

    public DeepSeek(NoDriver driver){
        this.driver = driver;
    }

    /**
     * Checks if the authentication process is complete by verifying the presence of a prompt input field.
     *
     * @return {@code true} if the authentication is done, otherwise {@code false}.
     */
    private Boolean isAuthDone(){
        var promptInput = driver.findElement(By.id("chat-input"));
        try{
            promptInput.waitToAppear(5, 100);
            return true;
        }
        catch (Exception ex){
            return false;
        }
    }

    /**
     * Authenticates the user by navigating through Google sign-in process.
     * @return {@code true} if the authentication is successful, otherwise {@code false}.
     */
    @Override
    public Boolean auth() {
        try{
            if (!driver.getNavigation().loadUrlAndBypassCFXDO(url, 5, 20))
                throw new Exception("Could not load URL or bypass CF challenge.");
            if (isAuthDone())
                return true;
            if (!bypassCustomCF())
                throw new Exception("Could not bypass custom CF challenge");

            var signInWithGoogleButton = driver.findElement(By.cssSelector("div.ds-button__icon"));
            signInWithGoogleButton.waitToAppear(5, 100);
            driver.getInput().emulateClick(signInWithGoogleButton);
            String previousTitle = driver.getTitle().orElseThrow(() -> new Exception("Can't get title"));
            if (!new LambdaWaitTask(() -> driver.getTitle().map(title -> !title.equalsIgnoreCase(previousTitle)).orElse(false)).execute(10, 200))
                throw new Exception("Could not load Google sign in page in time");

            if (!driver.getNavigation().loadUrlAndBypassCFXDO(null, null, 20))
                throw new Exception("Could not load URL or bypass CF challenge.");

            var selectGoogleAccountButton = driver.findElement(By.cssSelector("[data-email]"));
            selectGoogleAccountButton.waitToAppear(5, 100);
            driver.getInput().emulateClick(selectGoogleAccountButton);

            driver.findElement(By.cssSelector("button[data-idom-class]")).waitToAppear(5, 200);
            var declineAcceptButtons = driver.findElements(By.cssSelector("button[data-idom-class]"));
            if (declineAcceptButtons.isEmpty())
                throw new com.vityazev_egor.LLMs.Shared.ProviderException("Can't find decline and accept buttons");
            driver.getInput().emulateClick(declineAcceptButtons.get(1));
            Shared.sleep(1000);
            driver.getNavigation().waitFullLoad(5);

            return isAuthDone();
        } catch (Exception ex) {
            logger.error("Error occurred while authenticating: " + ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Sends a prompt to the chat and waits for an answer.
     *
     * @param prompt The user's input prompt.
     * @param timeOutForAnswer The maximum time (in seconds) to wait for an answer.
     * @return A {@link ChatAnswer} object containing the response text, HTML content, and screenshot of the answer.
     */
    @Override
    public ChatAnswer ask(String prompt, Integer timeOutForAnswer) {
        try {
            // MAKE SURE THAT CHAT IS OPENED
            Boolean isChatOpened = driver.getTitle().map(title -> title.contains("DeepSeek")).orElse(false);
            if (!isChatOpened)
                if (!auth())
                    throw new Exception("Could not open chat or authenticate.");

            if (!bypassCustomCF())
                throw new RuntimeException("Can't bypass custom CF challenge");

            // SEND PROMPT
            var chatInput = driver.findElement(By.id("chat-input"));
            var sendButton = driver.findElement(By.cssSelector("div[role='button'][aria-disabled='false']"));
            chatInput.waitToAppear(5, 100);
            driver.getInput().insertText(chatInput, prompt);
            sendButton.waitToAppear(5, 100);
            driver.getInput().emulateClick(sendButton);

            // GET ANSWER
            if (!com.vityazev_egor.LLMs.Shared.waitForAnswer(driver, timeOutForAnswer, 5000))
                throw new RuntimeException("Could not receive answer within the specified time limit.");
            var answerDivs = driver.findElements(By.cssSelector("div.ds-markdown.ds-markdown--block"));
            if (answerDivs.isEmpty())
                throw new RuntimeException("No answer received.");
            var lastestAnswer = answerDivs.get(answerDivs.size() - 1);
            return new ChatAnswer(
                lastestAnswer.getText(),
                lastestAnswer.getHTMLContent(),
                driver.getMisc().captureScreenshot()
            );
        }
        catch (Exception ex){
            logger.error("Error occurred while sending prompt: " + ex.getMessage(), ex);
            return new ChatAnswer();
        }
    }

    /**
     * Bypasses a custom CF challenge by clicking on various positions within the challenge area.
     *
     * @return {@code true} if the bypass is successful, otherwise {@code false}.
     */
    private Boolean bypassCustomCF(){
        var cfDiv = driver.findElement(By.id("cf-turnstile"));
        if (!com.vityazev_egor.LLMs.Shared.waitForElements(false, 3, cfDiv))
            return true;
        var bypassTask = new LambdaWaitTask(() ->{
            if (!cfDiv.isExists()) return true;
            try {
                var cfDivPos = cfDiv.getPosition().orElseThrow(() -> new RuntimeException("Can't get cfDiv position"));
                var cfDivSize = cfDiv.getSize().orElseThrow(() -> new RuntimeException("Can't get cfDiv size"));
                if (cfDivSize.getWidth() == 0.0) {
                    logger.info("Custom CF challenge is not visible");
                    return true;
                }
                // i don't know a specific position of challenge button, so i'm clicking everywhere
                for (double x = cfDivPos.getX() - cfDivSize.getWidth()/2; x <= cfDivPos.getX() + cfDivSize.getWidth()/2; x+=10.0){
                    driver.getXdo().click(x, cfDivPos.getY());
                }
            }
            catch (Exception ex){
                logger.error("Error occurred during CF bypass: " + ex.getMessage(), ex);
            }
            return false;
        });
        return bypassTask.execute(10, 100);
    }

    @Override
    public Boolean createNewChat() {
        return auth();
    }

    @Override
    public String getName() {
        return "DeepSeek";
    }
    
}
