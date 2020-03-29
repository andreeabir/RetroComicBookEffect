import socket
from python.comic_book_effect import ComicBookEffect


HOST = '127.0.0.1'  # Standard loopback interface address (localhost)
PORT = 65432        # Port to listen on (non-privileged ports are > 1023)


if __name__ == '__main__':
    comicBookEffect = ComicBookEffect()

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as serverSocket:
        serverSocket.bind((HOST, PORT))
        serverSocket.listen()

        while True:
            conn, addr = serverSocket.accept()
            with conn:
                print('Connected by', addr)
                data = conn.recv(1024*1024)
                if not data:
                    continue

                output_data = comicBookEffect.process(data)
                conn.sendall(output_data)