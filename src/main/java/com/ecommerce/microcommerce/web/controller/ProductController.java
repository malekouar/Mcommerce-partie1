package com.ecommerce.microcommerce.web.controller;

import com.ecommerce.microcommerce.dao.ProductDao;
import com.ecommerce.microcommerce.model.Product;
import com.ecommerce.microcommerce.web.exceptions.ProduitGratuitException;
import com.ecommerce.microcommerce.web.exceptions.ProduitIntrouvableException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Api(description = "API pour les opérations CRUD sur les produits.")

@RestController
public class ProductController {

    @Autowired
    private ProductDao productDao;


    //Récupérer la liste des produits

    @RequestMapping(value = "/Produits", method = RequestMethod.GET)

    public MappingJacksonValue listeProduits() {

        Iterable<Product> produits = productDao.findAll();

        SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");

        FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);

        MappingJacksonValue produitsFiltres = new MappingJacksonValue(produits);

        produitsFiltres.setFilters(listDeNosFiltres);

        return produitsFiltres;
    }


    //Récupérer un produit par son Id
    @ApiOperation(value = "Récupère un produit grâce à son ID à condition que celui-ci soit en stock!")
    @GetMapping(value = "/Produits/{id}")

    public Product afficherUnProduit(@PathVariable int id) {

        Product produit = productDao.findById(id);

        if(produit==null) throw new ProduitIntrouvableException("Le produit avec l'id " + id + " est INTROUVABLE. Écran Bleu si je pouvais.");

        return produit;
    }




    //ajouter un produit
    @PostMapping(value = "/Produits")
    public ResponseEntity<Void> ajouterProduit(@Valid @RequestBody Product product) throws ProduitGratuitException {

        Product productAdded =  productDao.save(product);

        if (productAdded == null) {

            return ResponseEntity.noContent().build();
        } else {

            if (productAdded.getPrix() == 0) throw new ProduitGratuitException("Un produit ne peut être gratuit !");
        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(productAdded.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping (value = "/Produits/{id}")
    public void supprimerProduit(@PathVariable int id) {

        productDao.delete(id);
    }

    @PutMapping (value = "/Produits")
    public void updateProduit(@RequestBody Product product) {

        if (product.getPrix() == 0)
            throw new ProduitGratuitException("Le produit id = " + product.getId() + " ne doit pas être gratuit !");

        productDao.save(product);
    }


    //Pour les tests
    @GetMapping(value = "test/produits/{prix}")
    public List<Product>  testeDeRequetes(@PathVariable int prix) {

        return productDao.chercherUnProduitCher(400);
    }


    //Calcul de la marge
    @ApiOperation(value = "calcule la marge de chaque produit (différence entre prix d‘achat et prix de vente).")
    @GetMapping(value = "/AdminProduits")
    public Map<Product, Integer> calculerMargeProduit() {

        List<Product> products = productDao.findAll();

        Map<Product, Integer> productsWithMarge = new HashMap<>();

        for (Product product : products) {

            productsWithMarge.put(product, product.getPrix() - product.getPrixAchat());

        }

        //on retourne une collection triée sur les produits
        return productsWithMarge.entrySet()
                .stream()
                .sorted((Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    //Tri par ordre alphabétique
    @ApiOperation(value = "fait appel à une méthode dans ProductDao  qui utilise le nommage conventionné de Spring Data JPA (findAllByOrderByNomAsc)")
    @GetMapping(value = "/TrierProduitsParNom")
    public List<Product> trierProduitsParOrdreAlphabetique() {

        return productDao.findAllByOrderByNomAsc();
    }

}
