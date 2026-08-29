package com.drobnyd.drobnyd.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.Client;
import com.drobnyd.drobnyd.entity.Operator;
import com.drobnyd.drobnyd.entity.Wallet;
import com.drobnyd.drobnyd.exception.AccountLinkException;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.ClientRepository;
import com.drobnyd.drobnyd.repository.OperatorRepository;
import com.drobnyd.drobnyd.repository.WalletRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final ClientRepository clientRepository;
    private final OperatorRepository operatorRepository;
    private final WalletRepository walletRepository;

    public UserService(
            ClientRepository clientRepository,
            OperatorRepository operatorRepository,
            WalletRepository walletRepository) {
        this.clientRepository = clientRepository;
        this.operatorRepository = operatorRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Client> findClientByFirebaseUid(String firebaseUid) {
        log.info("Looking up client by Firebase UID: {}", firebaseUid);
        return clientRepository.findByFirebaseUid(firebaseUid);
    }

    @Transactional(readOnly = true)
    public Optional<Operator> findOperatorByFirebaseUid(String firebaseUid) {
        log.info("Looking up operator by Firebase UID: {}", firebaseUid);
        return operatorRepository.findByFirebaseUid(firebaseUid);
    }

    @Transactional(readOnly = true)
    public Wallet getWalletForClient(Integer clientId) {
        log.info("Loading wallet for client: {}", clientId);
        return walletRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", clientId.toString()));
    }

    @Transactional
    public Client provisionClient(String firebaseUid, String email, String firstName, String lastName) {
        log.info("Provisioning local client for Firebase UID: {}", firebaseUid);

        clientRepository.findByFirebaseUid(firebaseUid).ifPresent(existing -> {
            throw new AccountLinkException("Client with Firebase UID already exists: " + firebaseUid);
        });
        clientRepository.findByEmail(email).ifPresent(existing -> {
            throw new AccountLinkException("Client with email already exists: " + email);
        });

        Client client = Client.provisioned(firebaseUid, email, firstName, lastName);
        Client savedClient = clientRepository.save(client);

        walletRepository.save(Wallet.initialize(savedClient));

        return savedClient;
    }
}