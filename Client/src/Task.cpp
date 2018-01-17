#include "../include/Task.h"

Task::Task(boost::mutex *mutex, ConnectionHandler *connectionHandler, std::atomic<bool> *pAtomic)
        : mutex(mutex), connectionHandler(connectionHandler), isLoggedIn(pAtomic) {
}

void Task::run() {
    while (1) {
        // We can use one of three options to read data from the server:
        // 1. Read a fixed number of characters
        // 2. Read a line (up to the newline character using the getline() buffered reader
        // 3. Read up to the null character
        std::string answer;
        // Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
        // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
        if (!connectionHandler->getLine(answer)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            return;
        }

        int len = answer.length();
        // A C string must end with a 0 char delimiter.  When we filled the answer buffer from the socket
        // we filled up to the \n char - we must make sure now that a 0 char is also present. So we truncate last character.
        answer.resize(len - 1);

        std::cout << answer << std::endl;
        if (answer == "ACK login succeeded")
            isLoggedIn->store(true);
        if (answer == "ACK signout succeeded") {
            connectionHandler->close();
            return;

        }
    }

}