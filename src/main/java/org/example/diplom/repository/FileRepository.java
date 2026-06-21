package org.example.diplom.repository;

import org.example.diplom.model.CloudFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<CloudFile, Long> {
    List<CloudFile> findAllByUsername(String username);
    Optional<CloudFile> findByUsernameAndFilename(String username, String filename);
    void deleteByUsernameAndFilename(String username, String filename);
}