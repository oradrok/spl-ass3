#include "../include/connectionHandler.h"
#include "../include/Task.h"

void readInput(const std::atomic<bool> &isLoggedIn, ConnectionHandler &connectionHandler);

/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    boost::mutex mutex;
    std::atomic<bool> isLoggedIn(false);
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        return 1;
    }
    Task task(&mutex, &connectionHandler, &isLoggedIn);
    boost::thread thread2(&Task::run, &task);
    //From here we will see the rest of the ehco client implementation:
    readInput(isLoggedIn, connectionHandler);
    thread2.join();
        return 0;
    }

void readInput(const std::atomic<bool> &isLoggedIn, ConnectionHandler &connectionHandler) {
    while (1) {
        const short bufsize = 1024;
        char buf[bufsize];

        std::cin.getline(buf, bufsize);
        std::__cxx11::string line(buf);
        int len = line.length();

        if (!connectionHandler.sendLine(line)) {
            return;
        }

        if(isLoggedIn.load()&& line == "SIGNOUT")
            return;
    }
}
