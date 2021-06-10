package pkp.hhu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pkp.hhu.place.Place;
import pkp.hhu.place.PlaceService;
import pkp.hhu.post.Post;
import pkp.hhu.post.PostService;
import pkp.hhu.user.User;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@Controller
public class PostController {
    @Value("${file.uploadDir}")
    private String uploadFolder;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private PostService postService;
    private PlaceService placeService;

    public PostController(PostService postService, PlaceService placeService) {
        this.postService = postService;
        this.placeService = placeService;
    }

    @GetMapping("/post")
    public String getPostForm(ModelMap modelMap) {
        modelMap.addAttribute("place", new Place());
        modelMap.addAttribute("post", new Post());
        return "redirect:/";
    }

    @PostMapping("/post")
    public String createProduct(@RequestParam("name") String placeName, @RequestParam("lat") BigDecimal placeLat, @RequestParam("lng") BigDecimal placeLng,
                                @RequestParam("direction") String placeDirection, @RequestParam("description") String placeDescription, Post post,
                                ModelMap modelMap, HttpServletRequest request, final @RequestParam("photo") MultipartFile photo) {

        try {
            //ustalamy jaka jest ścieżka to folderu przechowywania plików lokalnie (przed wysłaniem do mysql)
            String uploadDirectory = request.getServletContext().getRealPath(uploadFolder);
            log.info("UploadDirectory: " + uploadDirectory);
            //ustalamy skąd od klienta pobrać plik
            String fileName = photo.getOriginalFilename();
            String filePath = Paths.get(uploadDirectory, fileName).toString();
            log.info("FileName: " + photo.getOriginalFilename());
            if (fileName == null || fileName.contains("..")) {
                // modelMap.addAttribute("invalid", "Sorry! Filename contains invalid path sequence \" + fileName");
                log.warn("Sorry! Filename contains invalid path sequence!");
                return "redirect:/";
            }
            String[] names = placeName.split(",");
            String[] descriptions = placeDescription.split(",");

            try {  //sprawdzamy czy folder do pobrania tymczasowego lokalnie istnieje
                File dir = new File(uploadDirectory);
                if (!dir.exists()) {
                    log.info("Folder Created");
                    dir.mkdirs();       // jeśli nie, to go stwórz
                }
                // Save the file locally
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
                stream.write(photo.getBytes());
                stream.close();
            } catch (FileNotFoundException fileNotFoundException) {
                log.info("Nie załączono pliku! " + fileNotFoundException);
            } catch (Exception exception) {
                log.error("Exception: " + exception);
                exception.printStackTrace();
            }
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            modelMap.addAttribute("post", new Post());

            byte[] imageData = photo.getBytes();  //przypis odebrany strumień/plik do zmiennej image data (tablica bajtów)
            Place place = new Place();
            place.setName(names[0]);
            place.setPhoto(imageData);      // dopisz dane pliku do obiektu place
            place.setLat(placeLat);
            place.setLng(placeLng);
            place.setDescription(descriptions[0]);
            place.setDirection(placeDirection);
            place.setTimeAvg(post.getTime());
            place.setRateAvg(post.getRate());
            placeService.save(place);

            post.setUser(user);
            post.setDate(LocalDate.now());
            post.setPlace(place);

            postService.save(post);

            log.info("Place Saved. Attached photo: >>>" + fileName + "<<<. No photo if field empty.");

            return "redirect:/";
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception: " + e);
            return "redirect:/";
        }
    }

    @PostMapping("/addPost")
    public String addPost(@RequestParam("placeId") int placeId, @RequestParam("commentPost") String comment,
                          @RequestParam("timePost") int time, @RequestParam("ratePost") int rate) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Post post = new Post();
        post.setComment(comment);
        post.setTime(time);
        post.setRate(rate);
        post.setUser(user);
        post.setDate(LocalDate.now());
        Place place = placeService.findById(placeId);
        post.setPlace(place);
        postService.save(post);
        int timeAvg = postService.avgPlaceTime(placeId);
        float rateAvg = postService.avgPlaceRate(placeId);
        place.setTimeAvg(timeAvg);
        place.setRateAvg(rateAvg);
        placeService.save(place);
        return "redirect:/";
    }

    @GetMapping("/place")
    public String showPostbyId(@RequestParam(required = false) Integer id, ModelMap modelMap) {
        List<Post> posts = postService.findByPlaceId(id);
        modelMap.addAttribute("posts", posts);
        return "place";
    }

    @GetMapping("/post/coordinates")
    public String getCoordinatesPostForm(ModelMap modelMap) {
        modelMap.addAttribute("place", new Place());
        modelMap.addAttribute("post", new Post());
        return "redirect:/";
    }

    @PostMapping("/post/coordinates")
    public String postCoordinatesPostForm(ModelMap modelMap, @RequestBody @Validated Place place, BindingResult bindingResult) {
        modelMap.addAttribute("place", place);
        modelMap.addAttribute("post", new Post());
        return "redirect:/";
    }

}
