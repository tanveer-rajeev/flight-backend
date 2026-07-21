package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.tour.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByItineraryId(Long itineraryId);

    List<Meal> findByItineraryIdAndMealType(Long itineraryId, Meal.MealType mealType);

    void deleteByItineraryId(Long itineraryId);
}
