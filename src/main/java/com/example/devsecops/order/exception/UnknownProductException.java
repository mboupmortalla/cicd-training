package com.example.devsecops.order.exception;

/**
 * Le client a reference un produit qui n'existe pas au catalogue.
 * C'est son entree qui est fausse, pas la ressource appelee : 400, pas 404.
 */
public class UnknownProductException extends RuntimeException {
    public UnknownProductException(String message) {
        super(message);
    }
}
