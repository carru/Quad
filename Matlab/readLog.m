function [ latitude, longitude, altitude, accuracy ] = readLog( fileName )

    fd = fopen(fileName,'r');

    data = fscanf(fd,'%f;%f;%f;%f%*s');
    
    latitude = data(1:4:end)
    longitude = data(2:4:end)
    altitude = data(3:4:end);
    accuracy = data(4:4:end);

    fclose(fd);
    
end
