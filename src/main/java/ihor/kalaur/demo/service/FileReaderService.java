package ihor.kalaur.demo.service;

import java.util.List;

public interface FileReaderService<T> {
    List<T> readFromFile(String fileName);

}
