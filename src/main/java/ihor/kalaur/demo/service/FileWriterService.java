package ihor.kalaur.demo.service;

public interface FileWriterService<T> {
    void writeDataToFile(T data, String pathToFile);
}
