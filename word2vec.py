import gensim
import socket
import threading


def get_n_similarity(model, words1, words2):
    print(words1, " ", words2)
    return round(model.n_similarity(words1, words2), 4)

def get_similarity(model, words1, words2):
    print(words1, " ", words2)
    return round(model.similarity(words1[0], words2[0]), 4)


def filter_words(model, words):
    result = []
    num_del = 0
    print("in filtered");
    for word in words:
        if word in result:
            num_del += 1
        if word in model:
            result.append(word)
        else:
            num_del += 1
    return result, num_del


def listen_to_connection(model, connection, address):
    sent = False
    while True:
        try:
            print("Connection from: ", address)
            data = connection.recv(1024)
            words = data.decode('utf-8').strip().split(", ")
            print(words[0], words[1])

            words1, num_del1 = filter_words(model, words[0].split(" "))
            words2, num_del2 = filter_words(model, words[1].split(" "))
            score = 0
            print(model)
            if len(words1) == 0 or len(words2) == 0:
                print("One of the token lists is empty. Returned -1")
                score = -1
            elif len(words1) == 1 and len(words2) == 1:
                print("sim")
                score = get_similarity(model, words1, words2)
            else:
                print("n sim")
                score = get_n_similarity(model, words1, words2)
                # if any words were deleted enforce penalty?
            print("score: ", score)
            connection.send(str.encode(str(score)))
            sent = True
            # print("end")
        except Exception(e):
            print("Unexpected error occurred: ", str(e))
            print("node score returned: -1")
            if not sent:
                connection.send(str.encode(str(-1)))
        finally:
            print("finally")
            connection.close()
            return True


def run_word2vec_server(trained_model_path):
    # load Google's pre-trained word2vec model
    model = gensim.models.KeyedVectors.load_word2vec_format(trained_model_path, binary=True)
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ("localhost", 10000)
    print("start up on %s port %s" % server_address)
    sock.bind(server_address)
    sock.listen(5)

    while True:
        print("wait for a connection")
        connection, client_address = sock.accept()
        connection.settimeout(180)
        threading.Thread(target=listen_to_connection, args=(model, connection, client_address)).start()

def test_model(word1, word2, trainedModelPath):
    model = gensim.models.KeyedVectors.load_word2vec_format(trainedModelPath, binary=True)
    print("words: ", word1,word2)
    n_sim = model.n_similarity(word1, word2)
    sim = model.similarity(word1,word2)
    print("n_similarity: ",n_sim)
    print("similarity", sim)


if __name__ == '__main__':
    # test_model(word1="student", word2="author", trainedModelPath="model/GoogleNews-vectors-negative300.bin")
    run_word2vec_server(trained_model_path="model/GoogleNews-vectors-negative300.bin")












