package com.bingchat4urapp_server.bingchat4urapp_server.Controlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.bingchat4urapp_server.bingchat4urapp_server.Context;
import com.bingchat4urapp_server.bingchat4urapp_server.BgTasks.CommandsExecutor;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskModel;
import com.vityazev_egor.Wrapper.LLMproviders;



@Controller
public class GUIController {

    @Autowired
    private Context context;

    @Autowired
    private Utils utils;

    @Autowired
    private CommandsExecutor executor;

    private final Logger logger = LoggerFactory.getLogger(GUIController.class);

    @GetMapping("/")
    public ModelAndView main(){
        var model = new ModelAndView("main");

        var promtModels = context.findLatestFinishedPromtTasks();
        for (TaskModel currentPromt : promtModels) {
            if (currentPromt.result != null && currentPromt.result.length() >60){
                currentPromt.result = currentPromt.result.substring(0,60);
            }
        }
        model.addObject("latestPromts", promtModels);
        String aiInUse = executor.getWrapper().getWorkingLLM().map(llm -> { return llm.getChat().getName();}).orElse("None");
        model.addObject("aiInUse", aiInUse);
        return model;
    }

    @GetMapping("/auth")
    public ModelAndView auth() {
        // we need to provide list of avaibel models
        var authRequired = executor.getWrapper().getLlms().stream().filter(llm-> llm.getAuthRequired() && !llm.getAuthDone()).toList();
        return new ModelAndView("auth", "authRequired", authRequired);
    }

    @PostMapping("/auth")
    public String postMethodName(@RequestParam String login, @RequestParam String password) {
        var newTask = utils.createAuthTask(login, password);
        context.save(newTask);
        return "redirect:/task/" + newTask.id;
    }

    @GetMapping("/switchai")
    public String switchAi(){
        executor.setUseDuckDuck(!executor.getUseDuckDuck());
        return "redirect:/";
    }

    @GetMapping("/newchat")
    public String newChatGui(){
        var newTask = utils.createNewChatTask("3");
        context.save(newTask); 
        return "redirect:/task/" + newTask.id;
    }

    @PostMapping("/sendgui")
    public String sendGui(@RequestParam String promt){
        var newTask = utils.createPromtTask(promt, "120");
        context.save(newTask);
        return "redirect:/task/" + newTask.id;
    }
    

    @RequestMapping(value = "/task/{taskid}", method = RequestMethod.GET)
    public ModelAndView waitTask(@PathVariable("taskid") Integer taskId) {
        var taskModel = context.findById(taskId).orElse(null);

        // если авторизация или создание чата прошло успешна то сразу редериктим на страницу для отправки запросов
        if ((taskModel.type == 1 || taskModel.type == 3) && !taskModel.gotError && taskModel.isFinished){
            return new ModelAndView("redirect:/");
        }

        var view = new ModelAndView("taskwait");
        view.addObject("currentTask", taskModel);

        // Добовляем массив строк для того, чтобы построчно вывести ответ
        if (!taskModel.gotError && taskModel.isFinished && taskModel.result != null){
            view.addObject("answerLines", taskModel.result.split("\n"));
        }
        return view;
    }

    @GetMapping("providers")
    public ModelAndView getProviders() {
        var model = new ModelAndView("providers", "providers", executor.getWrapper().getLlms());
        String aiInUse = executor.getWrapper().getWorkingLLM().map(llm -> { return llm.getChat().getName();}).orElse("None");
        model.addObject("aiInUse", aiInUse);
        return model;
    }

    @GetMapping("providers/resetStates")
    public ModelAndView resetProvidersStates() {
        executor.getWrapper().resetErrorStates();
        return new ModelAndView("redirect:/providers");
    }

    @RequestMapping(value = "providers/setPrefered/{providerName}", method=RequestMethod.GET)
    public ModelAndView requestMethodName(@PathVariable("providerName") LLMproviders provider) {
        executor.getWrapper().setPreferredProvider(provider);
        logger.info("Updated provider: " + provider);
        return new ModelAndView("redirect:/providers");
    }
    
    
}
