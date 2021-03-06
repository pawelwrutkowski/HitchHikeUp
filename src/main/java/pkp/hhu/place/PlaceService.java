package pkp.hhu.place;

import org.springframework.stereotype.Service;
import pkp.hhu.post.PostRepository;

import java.util.List;
import java.util.Optional;


@Service
public class PlaceService {
    private PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public List<Place> findAll() {return placeRepository.findAll();}

    public void save(Place place) {
        placeRepository.save(place);
    }

    public void deleteById(Integer id) {
        placeRepository.deleteById(id);
    }

    public Optional<Place> getPlaceById(Integer id) {
        return placeRepository.findById(id);
    }

    public Place findById(Integer id){
        return placeRepository.findById(id).orElse(null);
    }

}
