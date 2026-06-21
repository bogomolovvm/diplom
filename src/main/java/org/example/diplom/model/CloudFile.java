package org.example.diplom.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "cloud_files",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"username", "filename"})
        }
)
@Getter
@Setter
public class CloudFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "hash", length = 128)
    private String hash;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    public void set() {

    }
}


