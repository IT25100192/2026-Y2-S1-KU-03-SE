package com.starvoicelanka.common.exception;

public class DuplicateVoteException extends ApiException {
    public DuplicateVoteException(String message) {
        super(409, message);
    }
}
