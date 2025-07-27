package com.vityazev_egor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.LLMs.DeepSeek;
import com.vityazev_egor.LLMs.DuckDuck;
import com.vityazev_egor.LLMs.Gemini;
import com.vityazev_egor.LLMs.OpenAI;
import com.vityazev_egor.Models.ChatAnswer;
import com.vityazev_egor.Models.LLM;

import lombok.Getter;
import lombok.Setter;

public class Wrapper {
    @Getter
    private final NoDriver driver;
    private final CustomLogger logger;
    @Getter
    private final List<LLM> llms;

    public enum LLMproviders{
        Copilot,
        DuckDuck,
        OpenAI,
        DeepSeek,
        Gemini
    }

    public enum WrapperMode{
        ExamMode, // we will try to get answer from any AI if selected one fails
        Normal // we will just return empty answer
    }

    // указываем какую ИИ будем использовать по умолчанию
    @Getter
    @Setter
    private LLMproviders preferredProvider;
    // указываем какой режим работы будет
    private final WrapperMode wrapperMode;

    public static Boolean emulateError = false;

    public Wrapper(String socks5Proxy, LLMproviders preferredProvider, WrapperMode wrapperMode) throws IOException{
        this.driver = new NoDriver(socks5Proxy);
        this.logger = new CustomLogger(Wrapper.class.getName());
        this.driver.getXdo().calibrate();
        this.llms = Arrays.asList(
            new LLM(new OpenAI(driver),false, LLMproviders.OpenAI),
            new LLM(new Gemini(driver), false, LLMproviders.Gemini),
            new LLM(new DuckDuck(driver),false, LLMproviders.DuckDuck),
            new LLM(new DeepSeek(driver), false, LLMproviders.DeepSeek)
        );
        this.preferredProvider = preferredProvider;
        this.wrapperMode = wrapperMode;
    }

    /**
     * Authenticates with the specified LLM provider using Google Account.
     *
     * @param provider The LLM provider to authenticate with..
     * @return {@code true} if authentication is successful, {@code false} otherwise.
     */
    public Boolean auth(LLMproviders provider){
        return llms.stream().filter(l -> l.getProvider() == provider).findFirst().map(l->{
            Boolean result = l.getChat().auth();
            l.setAuthDone(result);
            return result;
        }).orElse(false);
    }

    /**
     * Creates a new chat with the specified LLM provider.
     *
     * @param provider The LLM provider to create a chat with.
     * @return {@code true} if the chat is created successfully, {@code false} otherwise.
     */
    public Boolean createChat(LLMproviders provider){
        return llms.stream().filter(l -> l.getProvider() == provider).findFirst().map(l->{
            Boolean result = l.getChat().createNewChat();
            if (!result) l.setGotError(true);
            return result;
        }).orElse(false);
    }

    public ChatAnswer askLLM(LLMproviders provider, String prompt, Integer timeOutForAnswer){
        return llms.stream().filter(l -> l.getProvider() == provider).findFirst().map(llm->{
            return askLLM(llm, prompt, timeOutForAnswer);
        }).orElse(new ChatAnswer());
    }

    /**
     * Asks a specific LLM the specified prompt and returns the answer.
     *
     * @param llm The LLM to ask to.
     * @param prompt The question or prompt to ask the LLM.
     * @param timeOutForAnswer The maximum time in milliseconds to wait for an answer from the LLM.
     * @return A {@link ChatAnswer} object containing the response from the LLM, or an empty {@link ChatAnswer} if no valid provider is available or if the LLM does not respond within the timeout period.
     */
    private ChatAnswer askLLM(LLM llm, String prompt, Integer timeOutForAnswer){
        var answer = llm.getChat().ask(prompt, timeOutForAnswer);
        if (answer.getCleanAnswer().isEmpty()){
            llm.setGotError(true);
        }
        // if current answer is equals to previous answer then we can say that something went wrong
        if (answer.getCleanAnswer().isPresent() && answer.getCleanAnswer().get().equals(llm.getLastAnswer())){
            llm.setGotError(true);
            logger.error(llm.getChat().getName() + " returned the same answer as last answer", null);
            return new ChatAnswer();
        }
        answer.getCleanAnswer().ifPresent(llm::setLastAnswer);
        return answer;
    }

    /**
     * Asks a Large Language Model (LLM) the specified prompt and returns the answer.
     *
     * @param prompt The question or prompt to ask the LLM.
     * @param timeOutForAnswer The maximum time in milliseconds to wait for an answer from the LLM.
     * @return A {@link ChatAnswer} object containing the response from the LLM, or an empty {@link ChatAnswer} if no valid provider is available or if the LLM does not respond within the timeout period.
     */
    public ChatAnswer askLLM(String prompt, Integer timeOutForAnswer){
        if (Objects.requireNonNull(wrapperMode) == WrapperMode.ExamMode) {
            for (int i = 0; i < llms.size(); i++) {
                var workingLLM = getWorkingLLM();
                if (workingLLM.isEmpty()) {
                    logger.error("There is no working providers available", null);
                    return new ChatAnswer();
                }
                ChatAnswer answer = askLLM(workingLLM.get(), prompt, timeOutForAnswer);
                if (answer.getCleanAnswer().isEmpty()) {
                    logger.error("LLM " + workingLLM.get().getProvider().name() + " didn't answer", null);
                    continue;
                }
                answer.addPrefixToCleanAnswer(String.format("[Answer from %s provider]\n", workingLLM.get().getProvider().name()));
                return answer;
            }
        }
        return getWorkingLLM().map(llm -> askLLM(llm, prompt, timeOutForAnswer)).orElse(new ChatAnswer());
    }

    /**
     * Retrieves a working LLM based on the specified criteria.
     *
     * @return An {@link Optional} containing a working LLM, or an empty {@link Optional} if no suitable LLM is found.
     */
    public Optional<LLM> getWorkingLLM(){
        // Получаем все ИИшки, у которых требуется авторизация и она пройдена, или авторизация не требуется. И у которых не было ошибок
        var workingLLMs = llms.stream().filter(llm -> 
            (!llm.getAuthRequired() || llm.getAuthDone())
            && !llm.getGotError()
        ).toList();

        if (workingLLMs.isEmpty()){
            logger.error("There is no working LLM", null);
            return Optional.empty();
        }
        
        return workingLLMs.stream().filter(llm -> llm.getProvider() == preferredProvider).findFirst()
                .or(() -> Optional.ofNullable(workingLLMs.getFirst()));

    }

    public void resetErrorStates(){
        llms.forEach(llm -> llm.setGotError(false));
    }

    public void exit(){
        driver.exit();
    }
}
