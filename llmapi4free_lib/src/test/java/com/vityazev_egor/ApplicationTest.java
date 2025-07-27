package com.vityazev_egor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Wrapper.LLMproviders;
import com.vityazev_egor.Wrapper.WrapperMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

class ApplicationTest {
    
    private static final String PROXY_ADDRESS = "127.0.0.1:2080";
    private Wrapper wrapper;
    
    @AfterEach
    void tearDown() throws IOException {
        if (wrapper != null) {
            wrapper.exit();
        }
    }
    
    private Wrapper createWrapper(LLMproviders provider, WrapperMode mode) throws IOException {
        return new Wrapper(PROXY_ADDRESS, provider, mode);
    }

    @Test
    void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    void duckduckChatIsNotOpened() throws IOException{
        wrapper = createWrapper(LLMproviders.DuckDuck, WrapperMode.Normal);
        var answer = wrapper.askLLM("How are you today?",40);
        assertTrue(answer.getCleanAnswer().isPresent());
        System.out.println(answer.getCleanAnswer().get());
        answer = wrapper.askLLM("Write hello world in java", 40);
        System.out.println(answer.getCleanAnswer().get());
        assertTrue(answer.getCleanAnswer().isPresent());
    }

    @Test
    void testDuckDuckScreenShot() throws IOException{
        wrapper = createWrapper(LLMproviders.DuckDuck, WrapperMode.ExamMode);
        var answer = wrapper.askLLM("напиши формулу равноускоренного движения в физике",40);
        assertTrue(answer.getAnswerImage().isPresent());
    }

    @Test
    void copilotAuth() throws IOException{
        wrapper = createWrapper(LLMproviders.Copilot, WrapperMode.ExamMode);
        var result = wrapper.auth(LLMproviders.Copilot);
        assertTrue(result);
    }

    @Test
    void copilotAnswer() throws IOException{
        wrapper = createWrapper(LLMproviders.Copilot, WrapperMode.Normal);
        var result = wrapper.auth(LLMproviders.Copilot);
        assertTrue(result);
        var answer = wrapper.askLLM("Напиши формулу равноусоркенного движения в физике",60);
        assertTrue(answer.getCleanAnswer().isPresent());
        System.out.println(answer.getCleanAnswer());
        assertTrue(answer.getAnswerImage().isPresent());
    }

    @Test
    void testOpenAIAuth() throws IOException{
        wrapper = createWrapper(LLMproviders.OpenAI, WrapperMode.ExamMode);
        Boolean result = wrapper.createChat(LLMproviders.OpenAI);
        if (result) result = wrapper.auth(LLMproviders.OpenAI);
        Shared.sleep(5000);
        assertTrue(result);
    }

    @Test
    void testOpenAIChat() throws IOException{
        wrapper = createWrapper(LLMproviders.OpenAI, WrapperMode.Normal);
        wrapper.auth(LLMproviders.OpenAI);
        var answer = wrapper.askLLM("Can you write hello world in java?",100);
        System.out.println(answer.getCleanAnswer());
        assertTrue(answer.getCleanAnswer().isPresent());
        answer = wrapper.askLLM("Can you do the same but in C#", 100);
        System.out.println(answer.getCleanAnswer());
        assertTrue(answer.getCleanAnswer().isPresent());
    }

    @Test
    void testRotatingSystem() throws IOException{
        wrapper = createWrapper(LLMproviders.DuckDuck, WrapperMode.ExamMode);
        // мне надо самому тут мешать программе
        var result = wrapper.askLLM("Напиши hello world \n на Java", 120);
        System.out.println(result.getCleanAnswer());
        assertTrue(result.getCleanAnswer().isPresent());
    }

    @Test
    void testDeepSeek() throws IOException, InterruptedException{
        wrapper = createWrapper(LLMproviders.DeepSeek, WrapperMode.Normal);
        var answer = wrapper.askLLM("Напиши hello world \n на Java",60);
        System.out.println(answer.getCleanAnswer());
        assertTrue(answer.getCleanAnswer().isPresent());
    }

    @Test
    void deepSeekAuth() throws IOException{
        wrapper = createWrapper(LLMproviders.DeepSeek, WrapperMode.Normal);
        var result = wrapper.auth(LLMproviders.DeepSeek);
        assertTrue(result);
    }

    @Test
    void testGeminiAuth() throws IOException{
        wrapper = createWrapper(LLMproviders.Gemini, WrapperMode.Normal);
        var result = wrapper.auth(LLMproviders.Gemini);
        assertTrue(result);
    }

    @Test
    void testGemini() throws IOException{
        wrapper = createWrapper(LLMproviders.Gemini, WrapperMode.Normal);
        var answer = wrapper.askLLM("Напиши hello world \n на Java",120);
        System.out.println(answer.getCleanAnswer());
        assertTrue(answer.getCleanAnswer().isPresent());
    }
}
